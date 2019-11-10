package net.nnwsf.handler;

import io.undertow.util.AttachmentKey;

import java.util.*;

class URLMatcher {

    public static final AttachmentKey<URLMatcher> URL_MATCHER_ATTACHMENT_KEY = AttachmentKey.create(URLMatcher.class);

    private final String httpMethod;
    private final String[] pathElements;

    URLMatcher(String httpMethod, String path) {
        this.httpMethod = httpMethod.toUpperCase();
        Collection<String> pathElementCollection = new ArrayList<>();
        StringTokenizer pathTokenizer = new StringTokenizer(path, "/");
        while(pathTokenizer.hasMoreTokens()) {
            String nextToken = pathTokenizer.nextToken();
            if(nextToken != null && !"".equals(nextToken)) {
                pathElementCollection.add(nextToken);
            }
        }

        this.pathElements = pathElementCollection.toArray(new String[pathElementCollection.size()]);
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String[] getPathElements() {
        return pathElements;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + httpMethod.hashCode();
        result = prime * result + pathElements.length;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        URLMatcher other = (URLMatcher) obj;

        if (!Objects.equals(httpMethod, other.httpMethod)) {
            return false;
        }
        if (pathElements.length != other.pathElements.length) {
            return false;
        }
        for(int i=0; i<pathElements.length; i++) {
            if(!pathElements[i].startsWith("{") && !pathElements[i].endsWith("{") && !other.pathElements[i].startsWith("{") && !other.pathElements[i].endsWith("{") && !Objects.equals(pathElements[i], other.pathElements[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "URLMatcher [httpMethod=" + httpMethod + ", pathElements=" + Arrays.toString(pathElements) + "]";
    }


}
