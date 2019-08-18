package testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodeDetails {
    public String mainLabel;
    public int howMany;
    public String properties;
    public List<String> additionalLabels;

    public NodeDetails() {

    }

    public NodeDetails(Map<String, Object> source) {
        this.howMany = (int) source.get("howMany");
        this.properties = (String) source.get("properties");
        Object moreLabels = source.get("additionalLabels");
        if (moreLabels != null)
            this.additionalLabels = (List<String>) moreLabels;
        else {
            this.additionalLabels = new ArrayList<>();
        }
    }
}
