package com.moquality.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoboConfig {
    private Map<String, Map<String, Map<String, String>>> pom;
    private Map<String, String> mapping;
    private List<String> positiveFilter, negativeFilter;

    public RoboConfig(Map<String, String> config) {
       if(config.containsKey("pom")) {
           pom = parsePOM(config.get("pom"));
       }
       if(config.containsKey("mapping")) {
           mapping = parseMapping(config.get("mapping"));
       }
       // TODO: parse filters
    }

    private Map<String, String> parseMapping(String mapping) {
        Map<String, String> matching = new LinkedHashMap<>();
        try {
            JSONArray mapTable = new JSONArray(mapping);
            for(int i=0;i<mapTable.length();i++) {
                JSONArray item = mapTable.getJSONArray(i);
                if(item.length() > 1) {
                    matching.put(item.getString(0), item.getString(1));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return matching;
    }

    /**
     * // Page Object Model
     * {
     * 	"objects": [{
     * 		"class": "com.shauvik.calc.model.Calculator",
     * 		"methods": {
     * 			"clickNumber": {
     * 				"params": ["int"],
     * 				"returns": "Calculator"
     *                        },
     * 			"clickOperator": {
     * 				"params": ["java.lang.String"],
     * 				"returns": "Calculator"
     *            },
     * 			"clickEquals": {
     * 				"params": [],
     * 				"returns": "Calculator"
     *            },
     * 			"checkResult": {
     * 				"params": ["java.lang.String"],
     * 				"returns": "Calculator"
     *            }* 		}
     *    }]
     * }
     * @param pom
     * @return Class_Name, Map<Method_Name,Class_Name>
     */
    private Map<String, Map<String, Map<String, String>>> parsePOM(String pom) {
        // TODO: Consider using Jackson for auto converting JSON to models
        Map<String, Map<String, Map<String, String>>> pomMap = new HashMap<>();
        try {
            JSONObject model = new JSONObject(pom);
            JSONArray objects = model.getJSONArray("objects");
            for(int i=0; i<objects.length();i++) {
                JSONObject obj = objects.getJSONObject(i);
                String klass = obj.getString("class");
                Map<String, Map<String, String>> methodReturns = new HashMap<>();
                JSONObject methods = obj.getJSONObject("methods");
                Iterator<String> it = methods.keys();
                while(it.hasNext()){
                    String method = it.next();
                    JSONObject sigObj = methods.getJSONObject(method);
                    Map<String, String> mSignature = new HashMap<>();

                    JSONArray params = sigObj.getJSONArray("params");
                    StringBuffer sb = new StringBuffer();
                    for(int j=0;j<params.length();j++){
                        sb.append(params.get(j));
                        if(j<params.length()-1) sb.append(",");
                    }
                    mSignature.put("params", sb.toString());
                    mSignature.put("returns", sigObj.getString("returns"));
                    methodReturns.put(method, mSignature);
                }
                pomMap.put(klass, methodReturns);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return pomMap;
    }

    public Map<String, Map<String, Map<String, String>>> getPom() {
        return pom;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }
}
