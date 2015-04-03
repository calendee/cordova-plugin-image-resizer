package info.protonet.imageresizer;

import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.FileHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import android.util.Base64;
import android.content.Context;
import android.provider.MediaStore.Images.Media;
import android.net.Uri;
import android.os.Environment;

public class ImageResizer extends CordovaPlugin {
  private static final int ARGUMENT_NUMBER = 1;
  public CallbackContext callbackContext;

  private String uri;
  private String kitFileName;
  private String encodedString;
  private String folderName;
  private int quality;
  private int width;
  private int height;

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    try {
      this.callbackContext = callbackContext;

      if (action.equals("resize")) {
        checkParameters(args);

        // get the arguments
        JSONObject jsonObject = args.getJSONObject(0);
        uri = jsonObject.getString("uri");
        folderName = jsonObject.getString("folderName");
        quality = jsonObject.getInt("quality");
        width = jsonObject.getInt("width");
        height = jsonObject.getInt("height");

        // load the image from uri
        Bitmap bitmap = loadScaledBitmapFromUri(uri, width, height);

        // http://stackoverflow.com/a/4830846/75644
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object   
        byte[] b = baos.toByteArray(); 

        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        Log.i("Protonet", "encodedImage : " + encodedImage);

        // JSONObject result = new JSONObject("");
        // result.put("image", encodedImage);

        JSONObject result = new JSONObject();
          try {
            result.put("image", encodedImage);
          } catch (JSONException e) {
            Log.e("Protonet", "Failed to create JSON");
            e.printStackTrace();
          }        

        // // save the image as jpeg on the device
        // Uri scaledFile = saveFile(bitmap);
        // Log.i("Protonet", "scaledFile : " + scaledFile);

        // kitFileName = scaledFile.toString();

        // File finalFile = new File(kitFileName);

        // Log.i("Protonet", "kitFileName : " + kitFileName);
        // Log.i("Protonet", "finalFile : " + finalFile);

        // try {
        //   // Maybe try http://stackoverflow.com/a/15563584/75644 instead???
        //   // http://stackoverflow.com/a/17874349/75644
        //   InputStream inputStream = new FileInputStream(finalFile);//You can get an inputStream using any IO API
        //   byte[] bytes;
        //   byte[] buffer = new byte[8192];
        //   int bytesRead;
        //   ByteArrayOutputStream output = new ByteArrayOutputStream();
        //   try {
        //             Log.i("Protonet", "getting bytes :(");

        //       while ((bytesRead = inputStream.read(buffer)) != -1) {
        //         output.write(buffer, 0, bytesRead);
        //       }
        //   } catch (IOException e) {
        //     e.printStackTrace();
        //   }
          
        //   bytes = output.toByteArray();
        //   encodedString = Base64.encodeToString(bytes, Base64.DEFAULT);

        // } catch (FileNotFoundException e) {
        //   Log.e("Protonet", "kitFileName File not found. ::::::");
        // }
        

        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
        return true;
      } else {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
        return false;
      }
    } catch(JSONException e) {
    	Log.e("Protonet", "JSON Exception during the Image Resizer Plugin... :(");
    }
    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
    return false;
  }

  /**
   * Loads a Bitmap of the given android uri path
   *
   * @params uri the URI who points to the image
   **/
  private Bitmap loadScaledBitmapFromUri(String uriString, int width, int height) {
    try {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeStream(FileHelper.getInputStreamFromUriString(uriString, cordova), null, options);

      //calc aspect ratio
      int[] retval = calculateAspectRatio(options.outWidth, options.outHeight);

      options.inJustDecodeBounds = false;
      options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, width, height);
      Bitmap unscaledBitmap = BitmapFactory.decodeStream(FileHelper.getInputStreamFromUriString(uriString, cordova), null, options);
      return Bitmap.createScaledBitmap(unscaledBitmap, retval[0], retval[1], true);
    } catch (FileNotFoundException e) {
      Log.e("Protonet", "File not found. :(");
    } catch (IOException e) {
      Log.e("Protonet", "IO Exception :(");
    }catch(Exception e) {
      Log.e("Protonet", e.toString());
    }
    return null;
  }

  private Uri saveFile(Bitmap bitmap) {
    File folder = new File(Environment.getExternalStorageDirectory() + "/" + folderName);
    boolean success = true;
    if (!folder.exists()) {
      success = folder.mkdir();
    }

    if(success) {
      String fileName = "kit-temp.jpg";
      File file = new File(folder, fileName);
      if(file.exists()) file.delete();
      try {
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        out.flush();
        out.close();
      } catch(Exception e) {
        Log.e("Protonet", e.toString());
      }
      return Uri.fromFile(file);
    }
    return null;
  }

  /**
   * Figure out what ratio we can load our image into memory at while still being bigger than
   * our desired width and height
   *
   * @param srcWidth
   * @param srcHeight
   * @param dstWidth
   * @param dstHeight
   * @return
   */
  private int calculateSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
    final float srcAspect = (float)srcWidth / (float)srcHeight;
    final float dstAspect = (float)dstWidth / (float)dstHeight;

    if (srcAspect > dstAspect) {
        return srcWidth / dstWidth;
    } else {
        return srcHeight / dstHeight;
    }
  }

  /**
   * Maintain the aspect ratio so the resulting image does not look smooshed
   *
   * @param origWidth
   * @param origHeight
   * @return
   */
  private int[] calculateAspectRatio(int origWidth, int origHeight) {
      int newWidth = width;
      int newHeight = height;

      // If no new width or height were specified return the original bitmap
      if (newWidth <= 0 && newHeight <= 0) {
          newWidth = origWidth;
          newHeight = origHeight;
      }
      // Only the width was specified
      else if (newWidth > 0 && newHeight <= 0) {
          newHeight = (newWidth * origHeight) / origWidth;
      }
      // only the height was specified
      else if (newWidth <= 0 && newHeight > 0) {
          newWidth = (newHeight * origWidth) / origHeight;
      }
      // If the user specified both a positive width and height
      // (potentially different aspect ratio) then the width or height is
      // scaled so that the image fits while maintaining aspect ratio.
      // Alternatively, the specified width and height could have been
      // kept and Bitmap.SCALE_TO_FIT specified when scaling, but this
      // would result in whitespace in the new image.
      else {
          double newRatio = newWidth / (double) newHeight;
          double origRatio = origWidth / (double) origHeight;

          if (origRatio > newRatio) {
              newHeight = (newWidth * origHeight) / origWidth;
          } else if (origRatio < newRatio) {
              newWidth = (newHeight * origWidth) / origHeight;
          }
      }

      int[] retval = new int[2];
      retval[0] = newWidth;
      retval[1] = newHeight;
      return retval;
  }

  private boolean checkParameters(JSONArray args) {
    if (args.length() != ARGUMENT_NUMBER) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
        return false;
    }
    return true;
  }
}
