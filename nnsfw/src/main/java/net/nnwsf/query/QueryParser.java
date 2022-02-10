package net.nnwsf.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class QueryParser {

    private static final char[] QUOTES = {'"', '\''};

    private static final Collection<String> OPERATORS = Set.of("AND", "OR");

    private static QueryParser instance; 

    public static void init() {
        instance = new QueryParser();
    }

    public static  SearchTerm parseString(String aQueryString) {
        return instance.internalParseString(aQueryString);
    }

    private SearchTerm internalParseString(String aString) {
        if(aString == null || aString.trim().length() == 0) {
            return null;
        }
        SearchTerm searchTerm = null;
        String stringToParse = aString.trim();
        StringBuilder lastTerm = new StringBuilder();
        int i=0;
        while(i<=stringToParse.length()) {
            if(i == stringToParse.length() || (stringToParse.charAt(i) == ' ')) {
                if( lastTerm.length() > 0) {
                    String term = lastTerm.toString();
                    if(OPERATORS.contains(term)) {
                        if(searchTerm != null) {
                            if(searchTerm instanceof OperatorTerm) {
                                OperatorTerm operatorTerm = ((OperatorTerm)searchTerm);
                                if(!operatorTerm.getOperator().equalsIgnoreCase(term)) {
                                    throw new RuntimeException("Invalid search term '" + term + "' at position: " + i);
                                }
                            } else {
                                searchTerm = SearchTerm.operator(searchTerm, term);
                            }
                        } else {
                            throw new RuntimeException("Invalid search term '" + term + "' at position: " + i);
                        }
                    } else {
                        List<String> splitStrings = split(term, ':', QUOTES);
                        if(searchTerm == null) {
                            if(splitStrings.size() == 2) {
                                searchTerm = SearchTerm.keyValue(splitStrings.get(0), removeSurroundingQuotes(splitStrings.get(1)));
                            } else {
                                throw new RuntimeException("Invalid search term '" + term + "' at position: " + i);
                            }
                        } else {
                            if(splitStrings.size() == 2) {
                                if(searchTerm instanceof OperatorTerm) {
                                    OperatorTerm operatorTerm = ((OperatorTerm)searchTerm);
                                    operatorTerm.addValue(SearchTerm.keyValue(splitStrings.get(0), removeSurroundingQuotes(splitStrings.get(1))));
                                } else {
                                    throw new RuntimeException("Invalid search term '" + term + "' at position: " + i);
                                }
                            } else {
                                throw new RuntimeException("Invalid search term '" + term + "' at position: " + i);
                            }
                        }
                    }
                    lastTerm = new StringBuilder();
                }
                i++;
            } else if(stringToParse.charAt(i) == '"') {
                i = parseSubText(i, stringToParse, lastTerm, '"');
            } else if(stringToParse.charAt(i) == '\'') {
                i = parseSubText(i, stringToParse, lastTerm, '\'');
            } else if(stringToParse.charAt(i) == '(') {
                i = parseNestedTest(i, stringToParse, lastTerm, '(', ')');
                if(searchTerm != null) {
                    if(searchTerm instanceof OperatorTerm) {
                        OperatorTerm operatorTerm = (OperatorTerm)searchTerm;
                        operatorTerm.addValue(parseString(lastTerm.toString()));
                    } else {
                        throw new RuntimeException("Invalid search term '" + lastTerm + "' at position: " + i);
                    }
                } else {
                    searchTerm = parseString(lastTerm.toString());
                }
                lastTerm = new StringBuilder();
            } else {
                lastTerm.append(stringToParse.charAt(i));
                i++;
            }
        }
        return searchTerm;
    }

    int parseSubText(int index, String stringToParse, StringBuilder term, char endChar) {
        int i=index;
        while(i<stringToParse.length()) {
            term.append(stringToParse.charAt(i));
            i++;
            if(stringToParse.charAt(i) == endChar) {
                term.append(stringToParse.charAt(i));
                i++;
                break;
            }
        }
        return i;
    }

    private int parseNestedTest(int index, String stringToParse, StringBuilder term, char startChar, char endChar) {
        int i=index;
        int level = 0;
        while(i<stringToParse.length()) {
            if(stringToParse.charAt(i) == startChar) {
                level++;
            } else if(stringToParse.charAt(i) == endChar) {
                level--;
                if(level == 0) {
                    i++;
                    break;
                }
            } else {
                term.append(stringToParse.charAt(i));
            }
            i++;
        }
        return i;
    }

    private List<String> split(String stringToParse, char delimiter, char[] quotes) {
        List<String> splitValues = new ArrayList<>();
        int i=0;
        char firstQuote = 0;
        boolean inQuotes = false;
        StringBuffer term = new StringBuffer();
        while(i<stringToParse.length()) {
            char stringChar = stringToParse.charAt(i);
            if(inQuotes) {
                if(stringChar == firstQuote) {
                    inQuotes = false;
                }
                term.append(stringChar);
            } else {
                if(stringChar == delimiter) {
                    splitValues.add(term.toString());
                    term = new StringBuffer();
                } else {
                    if(firstQuote == 0) {
                        for(int j=0; j<quotes.length; j++) {
                            if(stringChar == quotes[j]) {
                                firstQuote = stringChar;
                                inQuotes = true;
                                break;
                            }
                        }
                    } else {
                        if(stringChar == firstQuote) {
                            inQuotes = true;
                            break;
                        }
                    }
                    term.append(stringChar);
                }
            }
            i++;
        }
        if(term.length() > 0) {
            splitValues.add(term.toString());
        }
        return splitValues;
    }

    private String removeSurroundingQuotes(String aString) {
        String result = aString;
        for(int i=0; i< QUOTES.length; i++) {
            if(result.charAt(0) == QUOTES[i]) {
                result = result.substring(1);
            }
            if(result.charAt(result.length()-1) == QUOTES[i]) {
                result = result.substring(1);
            }
        }
        return result;
    }
}
