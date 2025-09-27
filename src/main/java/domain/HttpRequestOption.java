package domain;

public class HttpRequestOption {
    private boolean needUrlEncodeQueryParam;

    public boolean isNeedUrlEncodeQueryParam() {
        return needUrlEncodeQueryParam;
    }

    public void setNeedUrlEncodeQueryParam(boolean needUrlEncodeQueryParam) {
        this.needUrlEncodeQueryParam = needUrlEncodeQueryParam;
    }

    public static class Builder {
        private final HttpRequestOption option;

        public Builder() {
            this.option = new HttpRequestOption();
        }

        public Builder ofNeedUrlEncodeQueryParam(boolean needUrlEncodeQueryParam) {
            option.needUrlEncodeQueryParam = needUrlEncodeQueryParam;
            return this;
        }

        public HttpRequestOption build() {
            return option;
        }
    }
}
