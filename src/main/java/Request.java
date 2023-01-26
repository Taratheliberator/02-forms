import org.apache.http.NameValuePair;

import java.util.List;

public class Request {

    private String method;
    private String path;
    private List<String> headers;
    private String body;

    public void setMethod(String method) {
        this.method = method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public void setBody(String body) {
        this.body = body;
    }

    private List<NameValuePair> queryParams;

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String getQueryParam(String name) {
        if(queryParams.isEmpty()) return null;

        for (NameValuePair param : queryParams){
            if (name.equals(param.getName())){
                System.out.println("Query param for " + name + " : "  + param.getValue());
                return param.getValue();
            }
        }
        System.out.println("No such query parameter");
        return null;
    }

    public List<NameValuePair> getQueryParams() {
        for (NameValuePair param : queryParams){
            System.out.println("Query name: " +
                    param.getName() + ", query param: " +
                    param.getValue());
        }
        return queryParams;
    }

    public void setQueryParams(List<NameValuePair> queryParams) {
        this.queryParams = queryParams;
    }
}
