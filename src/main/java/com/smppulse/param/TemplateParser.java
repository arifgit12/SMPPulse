package com.smppulse.param;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateParser {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^([\\w.:]+)(?:\\(([^)]*)\\))?$");

    public List<PlaceholderToken> parse(String template) {
        List<PlaceholderToken> tokens = new ArrayList<>();
        if (template == null || template.isEmpty()) return tokens;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            Matcher funcMatcher = FUNCTION_PATTERN.matcher(expression);

            String function;
            String[] args;

            if (funcMatcher.matches()) {
                function = funcMatcher.group(1);
                String argsStr = funcMatcher.group(2);
                args = argsStr != null ? argsStr.split(",") : new String[0];
            } else {
                function = expression;
                args = new String[0];
            }

            tokens.add(new PlaceholderToken(
                    matcher.group(0),
                    function,
                    args,
                    matcher.start(),
                    matcher.end()
            ));
        }
        return tokens;
    }

    public String resolve(String template, ParameterResolver resolver) {
        if (template == null || template.isEmpty()) return template;

        List<PlaceholderToken> tokens = parse(template);
        if (tokens.isEmpty()) return template;

        StringBuilder result = new StringBuilder(template);
        // Process in reverse order to maintain correct indices
        for (int i = tokens.size() - 1; i >= 0; i--) {
            PlaceholderToken token = tokens.get(i);
            String resolved = resolver.resolve(token);
            result.replace(token.startIndex(), token.endIndex(), resolved);
        }
        return result.toString();
    }
}
