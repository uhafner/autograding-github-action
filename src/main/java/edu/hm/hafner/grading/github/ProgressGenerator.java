package edu.hm.hafner.grading.github;

import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
public class ProgressGenerator {
    private static final String RED = "#D2222D";
    private static final String YELLOW = "#FFBF00";
    private static final String GREEN = "#238823";

    public static void main(final String[] args) {
        String options = """
                {
                  "series": [
                    {
                      "type": "pie",
                      "radius": ["75%", "100%"],
                      "avoidLabelOverlap": false,
                      "color": ["", "#D0D0D0"],
                      "hoverAnimation": false,
                      "label": {
                        "show": true,
                        "position": "center",
                        "fontSize": 55,
                        "fontWeight": "bold",
                        "color": "#000",
                        "formatter": ""
                      },
                      "itemStyle": {
                        "borderRadius": 10,
                        "borderColor": "#fff",
                        "borderWidth": 3
                      },
                      "labelLine": { "normal": { "show": false } },
                      "data": [ ]
                    }
                  ]
                }
                                """;
        final String script = """
          - name: Generate progress SVG
            id: chart
            uses: robiningelbrecht/apache-echarts-action@v1.1.0
            with:
              width: 200
              height: 200
              pass-options-as: string
              chart-option: ''
          - name: Save generated SVG
            run: |
              cat <<EOF > badges/progress.svg
              ${{ steps.chart.outputs.svg }}
              EOF
                """;

        for (int percentage = 0; percentage <= 100; percentage++) {
            var formatter = options.replace("\"formatter\": \"\"", String.format("\"formatter\": \"%d%%\"", percentage))
                    .replace("\"color\": [\"\"", String.format("\"color\": [\"%s\"",
                            percentage <= 50 ? RED : percentage <= 80 ? YELLOW : GREEN));

            var echarts = formatter.replace("\"data\": [ ]", createChart(percentage));
            var chart = StringUtils.deleteWhitespace(echarts);

            var stage = script.replace("chart-option: ''", String.format("chart-option: '%s'", chart))
                    .replace("badges/progress.svg", String.format("badges/%03d.svg", percentage))
                    .replace("id: chart", String.format("id: chart-%03d", percentage))
                    .replace("steps.chart.outputs", String.format("steps.chart-%03d.outputs", percentage));

            System.out.print(stage);
        }
    }

    private static String createChart(final int percentage) {
        var dataValues = new StringJoiner(",\n             ", "\"data\": [", "]");
        int i = 0;
        for (; i < ((percentage / 10) * 10) && i < 100; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"Filled\" }");
        }
        dataValues.add(String.format("{ \"value\": %d, \"name\": \"Filled\" }", percentage % 10));
        if (percentage != 100) {
            dataValues.add(String.format("{ \"value\": %d, \"name\": \"NotFilled\" }", (10 - percentage % 10)));
        }
        i += 10;
        for (; i < 100; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"NotFilled\" }");
        }

        return dataValues.toString();
    }
}
