package com.filestack.transforms;

import com.filestack.Config;
import com.filestack.FileLink;
import com.filestack.HttpException;
import com.filestack.StorageOptions;
import com.filestack.internal.CdnService;
import com.filestack.internal.Response;
import com.filestack.internal.Util;
import com.filestack.internal.responses.StoreResponse;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * {@link Transform Transform} subclass for image transformations.
 */
public class ImageTransform extends Transform {

  public ImageTransform(Config config, CdnService cdnService, String source, boolean isExternal) {
    super(cdnService, config, source, isExternal);
  }

  /**
   * Debugs the transformation as built so far, returning explanations of any issues.
   * @see <a href="https://www.filestack.com/docs/image-transformations/debug"></a>
   *
   * @return {@link JsonObject JSON} report for transformation
   * @throws HttpException on error response fro\m backend
   * @throws IOException           on network failure
   */
  public JsonObject debug() throws IOException {
    String tasksString = getTasksString();

    Response<JsonObject> response;
    if (isExternal) {
      response = cdnService.transformDebugExt(config.getApiKey(), tasksString, source);
    } else {
      response = cdnService.transformDebug(tasksString, source);
    }

    Util.checkResponseAndThrow(response);

    JsonObject body = response.getData();
    if (body == null) {
      throw new IOException();
    }

    return body;
  }

  /**
   * Stores the result of a transformation into a new file. Uses default storage options.
   * @see <a href="https://www.filestack.com/docs/image-transformations/store"></a>
   *
   * @return new {@link FileLink FileLink} pointing to the file
   * @throws HttpException on error response from backend
   * @throws IOException           on network failure
   */
  public FileLink store() throws IOException {
    return store(null);
  }

  /**
   * Stores the result of a transformation into a new file.
   * @see <a href="https://www.filestack.com/docs/image-transformations/store"></a>
   *
   * @param storageOptions configure where and how your file is stored
   * @return new {@link FileLink FileLink} pointing to the file
   * @throws HttpException on error response from backend
   * @throws IOException           on network failure
   */
  public FileLink store(@Nullable StorageOptions storageOptions) throws IOException {
    if (storageOptions == null) {
      storageOptions = new StorageOptions.Builder().build();
    }

    tasks.add(storageOptions.getAsTask());

    Response<StoreResponse> response;
    String tasksString = getTasksString();
    if (isExternal) {
      response = cdnService.transformStoreExt(config.getApiKey(), tasksString, source);
    } else {
      response = cdnService.transformStore(tasksString, source);
    }

    Util.checkResponseAndThrow(response);

    StoreResponse body = response.getData();
    if (body == null) {
      throw new IOException();
    }

    String handle = body.getUrl().split("/")[3];
    return new FileLink(config, handle);
  }

  /**
   * Add a new transformation to the chain. Tasks are executed in the order they are added.
   *
   * @param task any of the available {@link ImageTransformTask} subclasses
   */
  public ImageTransform addTask(ImageTransformTask task) {
    if (task == null) {
      throw new IllegalArgumentException("ImageTransformTask object cannot be null!");
    }
    tasks.add(task);
    return this;
  }
}
