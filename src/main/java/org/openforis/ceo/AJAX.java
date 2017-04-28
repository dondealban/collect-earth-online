package org.openforis.ceo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;
import spark.Request;
import spark.Response;
import java.net.URLDecoder;


public class AJAX {

    public static String clone(Request req, Response rsp) {
        return req.body();
    }

    private static String expandResourcePath(String filename) {
        return AJAX.class.getResource(filename).getFile();
    }

    public static String geodashId(Request req, Response res) {
        String geodashDataDir = expandResourcePath("/public/json/");

        try (FileReader projectFileReader = new FileReader(new File(geodashDataDir, "proj.json"))) {
            JsonParser parser = new JsonParser();
            JsonArray projects = parser.parse(projectFileReader).getAsJsonArray();

            Optional matchingProject = StreamSupport.stream(projects.spliterator(), false)
                    .map(project -> project.getAsJsonObject())
                    .filter(project -> req.params(":id").equals(project.get("projectID").getAsString()))
                    .map(project -> {
                        try (FileReader dashboardFileReader = new FileReader(new File(geodashDataDir, "dash-" + project.get("dashboard").getAsString() + ".json"))) {
                            return parser.parse(dashboardFileReader).getAsJsonObject();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .findFirst();

            if (matchingProject.isPresent()) {
                if (req.queryParams("callback") != null) {
                    return req.queryParams("callback") + "(" + matchingProject.get().toString() + ")";
                } else {
                    return matchingProject.get().toString();
                }
            } else {
                if (true) { // FIXME: Make sure this is true if the user has role admin
                    String newUUID = UUID.randomUUID().toString();

                    JsonObject newProject = new JsonObject();
                    newProject.addProperty("projectID", req.params(":id"));
                    newProject.addProperty("dashboard", newUUID);
                    projects.add(newProject);

                    try (FileWriter projectFileWriter = new FileWriter(new File(geodashDataDir, "proj.json"))) {
                        projectFileWriter.write(projects.toString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    JsonObject newDashboard = new JsonObject();
                    newDashboard.addProperty("projectID", req.params(":id"));
                    newDashboard.addProperty("projectTitle", req.queryParams("title"));
                    newDashboard.addProperty("widgets", "[]");
                    newDashboard.addProperty("dashboardID", newUUID);

                    try (FileWriter dashboardFileWriter = new FileWriter(new File(geodashDataDir, "dash-" + newUUID + ".json"))) {
                        dashboardFileWriter.write(newDashboard.toString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    if (req.queryParams("callback") != null) {
                        return req.queryParams("callback") + "(" + newDashboard.toString() + ")";
                    } else {
                        return newDashboard.toString();
                    }
                } else {
                    return "No project exists with ID: " + req.params(":id") + ".";
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static String UpdateDashBoardByID(Request req, Response res)
    {
        String returnString = "";

/* Code will go here to update dashboard*/


        return  returnString;
    }
    public static String CreateDashBoardWidgetByID(Request req, Response res) {
        String geodashDataDir = expandResourcePath("/public/json/");
        JsonParser parser = new JsonParser();
        JsonObject dashboardObj = new JsonObject();
        try (FileReader dashboardFileReader = new FileReader(geodashDataDir + "dash-" + req.queryParams("dashID") + ".json"))
        {
            dashboardObj = parser.parse(dashboardFileReader).getAsJsonObject();
            dashboardObj.getAsJsonArray("widgets").add((JsonObject) parser.parse(URLDecoder.decode(req.queryParams("widgetJSON"), "UTF-8")));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try(FileWriter dashReader = new FileWriter(geodashDataDir +"dash-" + req.queryParams("dashID") + ".json"))
        {
            dashReader.write(dashboardObj.toString());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (req.queryParams("callback") != null) {
            return req.queryParams("callback").toString() + "()";
        }
        else
        {
            return "";
        }
    }
    public static String UpdateDashBoardWidgetByID(Request req, Response res) {
        try{
            deleteOrUpdate(req.queryParams("dashID"), req.params(":id"), req.queryParams("widgetJSON"), Boolean.FALSE);
        }
        catch(Exception e) {
            throw new RuntimeException(e);//deleteOrUpdateLock(req.queryParams("dashID"), req.params(":id"), req.queryParams("widgetJSON"), Boolean.FALSE);
        }

        if (req.queryParams("callback") != null) {
            return req.queryParams("callback").toString() + "()";
        }
        else
        {
            return "";
        }

    }
    public static String DeleteDashBoardWidgetByID(Request req, Response res) {

        deleteOrUpdate(req.queryParams("dashID"), req.params(":id"), "", Boolean.TRUE);

        if (req.queryParams("callback") != null) {
            return req.queryParams("callback").toString() + "()";
        }
        else
        {
            return "";
        }
    }
    private static void deleteOrUpdate(String dashID, String ID, String widgetJSON, Boolean delete) {
        try {
            String geodashDataDir = expandResourcePath("/public/json/");
            if (geodashDataDir.indexOf("/") == 0) {
                geodashDataDir = geodashDataDir.substring(1);
            }
            JsonParser parser = new JsonParser();
            JsonObject dashboardObj = new JsonObject();
            JsonArray finalArr = new JsonArray();
            FileSystem fs = FileSystems.getDefault();
            Path path = fs.getPath(geodashDataDir + "dash-" + dashID + ".json");
            int retries = 0;
            while (retries < 200) {
                try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                    FileLock lock = fileChannel.tryLock();
                    ByteBuffer buffer = ByteBuffer.allocate(2000);
                    int noOfBytesRead = fileChannel.read(buffer);
                    String jsonString = "";
                    while (noOfBytesRead != -1) {
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            jsonString += (char) buffer.get();
                        }
                        buffer.clear();
                        noOfBytesRead = fileChannel.read(buffer);
                    }
                    dashboardObj = parser.parse(jsonString).getAsJsonObject();
                    JsonArray widgets = dashboardObj.getAsJsonArray("widgets");
                    for (int i = 0; i < widgets.size(); i++) {  // **line 2**
                        JsonObject childJSONObject = (JsonObject) widgets.get(i);
                        String wID = childJSONObject.get("id").getAsString();
                        if (wID.equals(ID)) {
                            if (!delete) {
                                JsonParser widgetParser = new JsonParser();
                                childJSONObject = (JsonObject) widgetParser.parse(URLDecoder.decode(widgetJSON, "UTF-8"));
                                finalArr.add(childJSONObject);
                            }
                        } else {
                            finalArr.add(childJSONObject);
                        }
                    }
                    dashboardObj.remove("widgets");
                    dashboardObj.add("widgets", finalArr); //dashboardObj.put("widgets", finalArr);
                    byte[] inputBytes = dashboardObj.toString().getBytes();
                    ByteBuffer buffer2 = ByteBuffer.wrap(inputBytes);
                    fileChannel.truncate(0);
                    fileChannel.write(buffer2);
                    fileChannel.close();
                    retries = 201;
                } catch (Exception e) {
                    retries++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}