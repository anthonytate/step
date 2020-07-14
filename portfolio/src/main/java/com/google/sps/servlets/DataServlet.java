// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesService.OutputEncoding;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.InputSettings;
import com.google.appengine.api.images.InputSettings.OrientationCorrection;
import com.google.appengine.api.images.OutputSettings;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.images.ServingUrlOptions.Builder;
import com.google.appengine.api.images.Transform;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.SortExpression;
import com.google.appengine.api.search.SortOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Entity taskEntity = new Entity("Task");
    taskEntity.setProperty("comment", request.getParameter("comment"));
    UserService userService = UserServiceFactory.getUserService();
    taskEntity.setProperty("email", userService.getCurrentUser().getEmail());
    taskEntity.setProperty("timestamp", System.currentTimeMillis());

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key entityKey = datastore.put(taskEntity);
    try {
      taskEntity = datastore.get(entityKey);
    } catch (EntityNotFoundException e) {
      response.sendRedirect("/script.js");
    }
    String imageUrl = getUploadedFileUrl(request, "comment-image", entityKey.getId());
    taskEntity.setProperty("image", imageUrl);
    datastore.put(taskEntity);

    response.sendRedirect("/index.html");
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query("Task").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    int maxComments;
    try {
      maxComments = Integer.parseInt(request.getParameter("max-comments"));
    } catch (NumberFormatException e) {
      maxComments = 5;
    }
    ArrayList<Comment> comments = new ArrayList<>();
    for (Entity entity : results.asIterable(FetchOptions.Builder.withLimit(maxComments))) {
      long id = entity.getKey().getId();
      String email = (String) entity.getProperty("email");
      String content = (String) entity.getProperty("comment");
      String image = (String) entity.getProperty("image");
      Comment comment = new Comment(id, email, content, image);
      comments.add(comment);
    }

    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

  private String convertToJson(ArrayList<String> list) {
    Gson gson = new Gson();
    String json = gson.toJson(list);
    return json;
  }

  /** Returns a URL that points to the uploaded file, or null if the user didn't upload a file. */
  private String getUploadedFileUrl(
      HttpServletRequest request, String formInputElementName, long id) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get(formInputElementName);

    // User submitted form without selecting a file, so we can't get a URL. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return "";
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so we can't get a URL. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return "";
    }

    ImagesService imagesService = ImagesServiceFactory.getImagesService();

    // Transform if image is portrait
    Image image = ImagesServiceFactory.makeImageFromBlob(blobKey);
    ImagesService.OutputEncoding outputEncoding = ImagesService.OutputEncoding.JPEG;
    InputSettings inputSettings = new InputSettings();
    InputSettings.OrientationCorrection orientationCorrection =
        InputSettings.OrientationCorrection.CORRECT_ORIENTATION;
    inputSettings.setOrientationCorrection(orientationCorrection);
    OutputSettings outputSettings = new OutputSettings(outputEncoding);
    try {
      image = imagesService.applyTransform(
          ImagesServiceFactory.makeResize(300, 300), image, inputSettings, outputSettings);
    } catch (IllegalArgumentException e) {
      image = imagesService.applyTransform(ImagesServiceFactory.makeRotate(0), image);
    }

    GcsService gcsService = GcsServiceFactory.createGcsService(new RetryParams.Builder()
                                                                   .initialRetryDelayMillis(10)
                                                                   .retryMaxAttempts(10)
                                                                   .totalRetryPeriodMillis(15000)
                                                                   .build());

    String bucket = "adtate-step-2020.appspot.com";
    String fileName = id + ".jpeg";
    try {
      gcsService.createOrReplace(new GcsFilename(bucket, fileName),
          new GcsFileOptions.Builder().mimeType("image/jpeg").build(),
          ByteBuffer.wrap(image.getImageData()));
    } catch (IOException e) {
      return "createOrReplace_error";
    }

    ServingUrlOptions options =
        ServingUrlOptions.Builder.withGoogleStorageFileName("/gs/" + bucket + "/" + fileName);

    return imagesService.getServingUrl(options);
  }
}
