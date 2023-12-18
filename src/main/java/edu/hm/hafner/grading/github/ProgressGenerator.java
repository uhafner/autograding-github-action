package edu.hm.hafner.grading.github;

import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
public class ProgressGenerator {
    public static void main(final String[] args) {
        String options = """
                {
                  "series": [
                    {
                      "type": "pie",
                      "radius": ["75%", "100%"],
                      "avoidLabelOverlap": false,
                      "color": ["#D2222D", "#FFBF00", "#238823", "#D0D0D0"],
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

        for (int percentage = 0; percentage <= 50; percentage++) {
            var formatter = options.replace("\"formatter\": \"\"", String.format("\"formatter\": \"%d%%\"", percentage));

            String data;
            if (percentage <= 50) {
                data = createRedChart(percentage);
            }
            else if (percentage <= 80) {
                data = createYellowChart(percentage);
            }
            else {
                data = createGreenChart(percentage);
            }
            var echarts = formatter.replace("\"data\": [ ]", data);
            var chart = StringUtils.deleteWhitespace(echarts);

            var stage = script.replace("chart-option: ''", String.format("chart-option: '%s'", chart))
                    .replace("badges/progress.svg", String.format("badges/%03d.svg", percentage))
                    .replace("id: chart", String.format("id: chart-%03d", percentage))
                    .replace("steps.chart.outputs", String.format("steps.chart-%03d.outputs", percentage));

            System.out.print(stage);
        }
    }

    private static String createGreenChart(final int percentage) {
        var dataValues = new StringJoiner(",\n             ", "\"data\": [", "]");
        int i = 0;
        for (; i < 50; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"Red\" }");
        }
        for (; i < 80; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"Yellow\" }");
        }
        for (; i < ((percentage / 10) * 10) && i < 100; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"Green\" }");
        }
        dataValues.add(String.format("{ \"value\": %d, \"name\": \"Green\" }", percentage % 10));
        if (percentage != 100) {
            dataValues.add(String.format("{ \"value\": %d, \"name\": \"NotFilled\" }", (10 - percentage % 10)));
        }
        i += 10;
        for (; i < 100; i += 10) {
            dataValues.add("{ \"value\": 10, name: 'NotFilled' }");
        }

        return dataValues.toString();
    }

    private static String createYellowChart(final int percentage) {
        var dataValues = new StringJoiner(",\n             ", "\"data\": [", "]");
        int i = 0;
        for (; i < 50; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"Red\" }");
        }
        for (; i < ((percentage / 10) * 10) && i < 80; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"Yellow\" }");
        }
        dataValues.add(String.format("{ \"value\": %d, \"name\": \"Yellow\" }", percentage % 10));
        dataValues.add("{ \"value\": 0, \"name\": \"Green\" }");
        dataValues.add(String.format("{ \"value\": %d, \"name\": \"NotFilled\" }", (10 - percentage % 10)));
        i += 10;
        for (; i < 100; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"NotFilled\" }");
        }

        return dataValues.toString();
    }

    private static String createRedChart(final int percentage) {
        var dataValues = new StringJoiner(",\n             ", "\"data\": [", "]");

        int i = 0;
        for (; i < ((percentage / 10) * 10) && i < 50; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"Red\" }");
        }
        dataValues.add(String.format("{ \"value\": %d, \"name\": \"Red\" }", percentage % 10));
        dataValues.add("{ \"value\": 0, \"name\": \"Yellow\" }");
        dataValues.add("{ \"value\": 0, \"name\": \"Green\" }");
        dataValues.add(String.format("{ \"value\": %d, \"name\": \"NotFilled\" }", (10 - percentage % 10)));
        i += 10;
        for (; i < 100; i += 10) {
            dataValues.add("{ \"value\": 10, \"name\": \"NotFilled\" }");
        }
        return dataValues.toString();
    }
}
