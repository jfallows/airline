package com.github.rvesse.airline.parser.options;

import java.util.List;
import java.util.regex.Pattern;

import com.github.rvesse.airline.Context;
import com.github.rvesse.airline.TypeConverter;
import com.github.rvesse.airline.model.OptionMetadata;
import com.github.rvesse.airline.parser.ParseOptionUnexpectedException;
import com.github.rvesse.airline.parser.ParseState;
import com.google.common.collect.PeekingIterator;

public class ClassicGetOptParser extends AbstractOptionParser {
    private static final Pattern SHORT_OPTIONS_PATTERN = Pattern.compile("-[^-].*");

    public ParseState parseOptions(PeekingIterator<String> tokens, ParseState state, List<OptionMetadata> allowedOptions) {
        if (!SHORT_OPTIONS_PATTERN.matcher(tokens.peek()).matches()) {
            return null;
        }

        // remove leading dash from token
        String remainingToken = tokens.peek().substring(1);

        ParseState nextState = state;
        while (!remainingToken.isEmpty()) {
            char tokenCharacter = remainingToken.charAt(0);

            // is the current token character a single letter option?
            OptionMetadata option = findOption(state, allowedOptions, "-" + tokenCharacter);
            if (option == null) {
                return null;
            }

            nextState = nextState.pushContext(Context.OPTION).withOption(option);

            // remove current token character
            remainingToken = remainingToken.substring(1);

            // for no argument options, process the option and remove the
            // character from the token
            if (option.getArity() == 0) {
                nextState = nextState.withOptionValue(option, Boolean.TRUE).popContext();
                continue;
            }

            if (option.getArity() == 1) {
                // we must, consume the current token so we can see the next
                // token
                tokens.next();

                // if current token has more characters, this is the value;
                // otherwise it is the next token
                if (!remainingToken.isEmpty()) {
                    checkValidValue(option, remainingToken);
                    Object value = TypeConverter.newInstance().convert(option.getTitle(), option.getJavaType(),
                            remainingToken);
                    nextState = nextState.withOptionValue(option, value).popContext();
                } else if (tokens.hasNext()) {
                    String tokenStr = tokens.next();
                    checkValidValue(option, tokenStr);
                    Object value = TypeConverter.newInstance().convert(option.getTitle(), option.getJavaType(),
                            tokenStr);
                    nextState = nextState.withOptionValue(option, value).popContext();
                }

                return nextState;
            }

            throw new ParseOptionUnexpectedException("Short options style can not be used with option ", option);
        }

        // consume the current token
        tokens.next();

        return nextState;
    }
}