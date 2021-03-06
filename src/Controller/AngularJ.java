/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller;

import Annotation.Component;
import Utills.Statics;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import static java.lang.reflect.Array.get;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.misc.IOUtils;

/**
 *
 * @author TheJenos
 */
public class AngularJ {

    HttpServletRequest Request;
    HttpServletResponse Response;
    public static boolean DEBUG = true;
    String Package;

    /*
        Connect object and sevelt
     */
    void start() {
        try {
            JSONArray jarray = new JSONArray();
            for (Class allClasse : getAllClasses(Package)) {
                AngularJComponent el = (AngularJComponent) allClasse.newInstance();
                Component comp = (Component) allClasse.getAnnotation(Component.class);
                JSONObject jo = new JSONObject();
                if (!comp.Selector().equals("")) {
                    if (comp.Selector().substring(0, 1).matches("\\b[A-Z]") || comp.Selector().split(" ").length > 1) {
                        throw new Exception("Directive/Component name '" + comp.Selector() + "' is invalid. The name should not contain leading or trailing whitespaces");
                    }
                    jo.put("selector", comp.Selector());
                } else {
                    if (allClasse.getSimpleName().substring(0, 1).matches("\\b[A-Z]")) {
                        throw new Exception("Directive/Component name '" + allClasse.getSimpleName() + "' is invalid. The name should not contain leading or trailing whitespaces");
                    }
                    jo.put("selector", allClasse.getSimpleName());
                }
                JSONObject scope = new JSONObject();
                JSONArray scopeval = new JSONArray();
                for (String string : comp.Scope()) {
                    scope.put(string.split(":")[0], string.split(":")[1]);
                    scopeval.put(string.split(":")[0]);
                }
                jo.put("scope", scope);
                jo.put("scopeval", scopeval);
                jo.put("replace", comp.Replace());
                jo.put("transclude", comp.Transclude());
                jo.put("restrict", comp.Restrict());
                if (!comp.Controller().equals("")) {
                    BufferedReader br = new BufferedReader(new FileReader(Thread.currentThread().getContextClassLoader().getResource(Package.replace('.', '/') + "/" + jo.getString("selector") + "/" + comp.Controller()).getFile()));
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();
                    while (line != null) {
                        sb.append(line);
                        line = br.readLine();
                    }
                    jo.put("controller", sb.toString());
                }
                if (comp.ControllerPara().length > 0) {
                    jo.put("controllerpara", comp.ControllerPara());
                }
                if (!comp.TemplateURL().equals("")) {
                    jo.put("templateUrl", Request.getRequestURI() + "?file=" + jo.getString("selector") + URLEncoder.encode("/", "UTF-8") + comp.TemplateURL());
                } else {
                    jo.put("template", el.template());
                }
                jarray.put(jo);

            }
            Response.getWriter().println(printDirectives(jarray));
        } catch (Exception ex) {
            if (DEBUG) {
                ex.printStackTrace();
            }
        }
    }

    private void getFile(String parameter) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(Thread.currentThread().getContextClassLoader().getResource(Package.replace('.', '/') + "/" + parameter).getFile()));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            Response.getWriter().write(sb.toString());
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    public String printDirectives(JSONArray array) {
        String ret = Statics.js + "angular.module(\"angularj\", [])";
        for (int i = 0; i < array.length(); i++) {
            JSONObject get = array.getJSONObject(i);
            String para = "";
            if (get.has("controllerpara")) {
                for (int j = 0; j < get.getJSONArray("controllerpara").length(); j++) {
                    String paralines = get.getJSONArray("controllerpara").getString(j);
                    para += "," + paralines;
                }
            }
            String inputs = "";
            for (int j = 0; j < get.getJSONArray("scopeval").length(); j++) {
                String paralines = get.getJSONArray("scopeval").getString(j);
                inputs += paralines + ":angular.copy($scope." + paralines + "),";
            }
            ret += ".directive(\"" + get.getString("selector") + "\", function () {"
                    + "return {"
                    + "scope: " + get.getJSONObject("scope") + ","
                    + "restrict: '" + get.getString("restrict") + "',"
                    + "replace: " + get.getBoolean("replace") + ","
                    + "controller : function($scope" + para + "){$scope.$server = new JSframwork(\"AngulerJ?Controller=" + get.getString("selector") + "\",$scope);" + get.getString("controller") + "},"
                    + (get.has("template") ? "template: '" + get.getString("template") + "',\n" : "")
                    + (get.has("templateUrl") ? "templateUrl: '" + get.getString("templateUrl") + "',\n" : "")
                    + "transclude: '" + get.getBoolean("transclude") + "'"
                    + "};"
                    + "})";
        }
        return ret;
    }

    public Class[] getAllClasses(String pckgname) {
        try {
            ArrayList classes = new ArrayList();
            File directory = null;
            try {
                directory = new File(Thread.currentThread().getContextClassLoader().getResource(pckgname.replace('.', '/')).getFile());
            } catch (NullPointerException x) {
                throw new ClassNotFoundException(pckgname + " does not appear to be a valid package");
            }
            if (directory.exists()) {
                String[] files = directory.list();
                for (int i = 0; i < files.length; i++) {
                    File f = new File(directory, files[i]);
                    if (f.isDirectory() && f.list().length > 0) {
                        classes.add(Class.forName(pckgname + '.' + files[i] + '.' + files[i]));
                    }
                }
            } else {
                throw new ClassNotFoundException(pckgname + " does not appear to be a valid package");
            }
            Class[] classesA = new Class[classes.size()];
            classes.toArray(classesA);

            return classesA;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public AngularJ(HttpServletRequest Request, HttpServletResponse Response, boolean DEBUG, String Package) {
        this.Request = Request;
        this.Response = Response;
        this.DEBUG = DEBUG;
        this.Package = Package;
        if (Request.getParameter("file") != null) {
            this.getFile(Request.getParameter("file"));
        } else {
            this.start();
        }
    }

}
