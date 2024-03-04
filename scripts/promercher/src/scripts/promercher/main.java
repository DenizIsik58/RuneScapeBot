package scripts.promercher;

import dax.shared.jsonSimple.parser.ParseException;
import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tribot.script.sdk.Log;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;


public class main {
    public static String osrsItemInfoURL = "https://prices.runescape.wiki/api/v1/osrs/mapping";
    public static String lastFiveMinuteGEActivityURL = "https://prices.runescape.wiki/api/v1/osrs/5m";
    public static JSONArray osrsItemInfo;

    public static String getRequest(String targetUrl) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .build();


        return client.send(request,
                HttpResponse.BodyHandlers.ofString()).body();
    }

    public static void initiateGrandExchangeItemInfo() {
        try {
            osrsItemInfo = new JSONArray(getRequest(osrsItemInfoURL));
        }catch (Exception e){
            Log.debug(e);
        }
    }

    public static JSONArray getGrandExchangeItemInfo() {
        return osrsItemInfo;
    }

    public static JSONObject getLastFiveMinuteGEActivityURL() throws IOException, InterruptedException {
        return new JSONObject(getRequest(lastFiveMinuteGEActivityURL));
    }

    public static JSONArray parseAndReturnPossibleMerges(boolean removeMemberItems) throws IOException, InterruptedException, ParseException {
        JSONArray itemInfo = getGrandExchangeItemInfo();
        JSONObject lastFiveMinuteData = getLastFiveMinuteGEActivityURL().getJSONObject("data");
        JSONArray merges = new JSONArray();

        Map<Integer, Integer> usableIds = new HashMap<>();

        // Extract usable IDs
        itemInfo.forEach(item -> {
            JSONObject itemInformation = (JSONObject) item;
            if (itemInformation.has("limit")) {
                if (!removeMemberItems || !itemInformation.getBoolean("members")) {
                    usableIds.put(itemInformation.getInt("id"), itemInformation.getInt("limit"));
                }
            }
        });

        // Iterate through lastFiveMinuteData and construct merges
        lastFiveMinuteData.keySet().forEach(key -> {
            JSONObject currentItemData = lastFiveMinuteData.getJSONObject(key);
            if (currentItemData.get("avgLowPrice") instanceof Integer &&
                    currentItemData.get("avgHighPrice") instanceof Integer &&
                    currentItemData.get("lowPriceVolume") instanceof Integer) {
                int itemId = Integer.parseInt(key);
                usableIds.entrySet().stream()
                        .filter(entry -> entry.getKey() == itemId)
                        .findFirst()
                        .ifPresent(entry -> {
                            int highPrice = currentItemData.getInt("avgHighPrice");
                            int lowPrice = currentItemData.getInt("avgLowPrice") + 1;
                            int lowPriceVolume = currentItemData.getInt("lowPriceVolume");
                            int margin = (highPrice - lowPrice) - 1;
                            int limit = entry.getValue();
                            if (margin > 0) {
                                JSONObject merge = new JSONObject()
                                        .put("id", entry.getKey())
                                        .put("limit", limit)
                                        .put("margin", margin)
                                        .put("price", lowPrice)
                                        .put("lowPriceVolume", lowPriceVolume);
                                merges.put(merge);
                            }
                        });
            }
        });

        return merges;
    }

    public static JSONObject findItemByEntryPrice(JSONArray jsonArray, int coins) {
        for (Object obj : jsonArray) {
            if (obj instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) obj;
                int price = jsonObject.getInt("price");
                if (price <= coins) {
                    return jsonObject;
                }
            }
        }
        return null;
    }



    public static JSONArray sortAndGetHighestMarginItem() {
        try {
            JSONArray items = parseAndReturnPossibleMerges(true);
            List<JSONObject> jsonObjectList = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                jsonObjectList.add(items.getJSONObject(i));
            }

            // Sort the list based on the "margin" value in descending order
            jsonObjectList.sort(Comparator.comparingInt(o -> -o.getInt("margin")));

            // Convert the sorted list back to a JSONArray
            return new JSONArray(jsonObjectList);
        }catch (Exception e){
            Log.debug(e);
            return null;
        }
    }

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
        initiateGrandExchangeItemInfo();
        System.out.println(findItemByEntryPrice(parseAndReturnPossibleMerges(true), 300));
    }
}
