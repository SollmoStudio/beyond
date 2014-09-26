/* global RequestInternal */
exports.Request = function (req) {
    var request = new RequestInternal(req);
    this.bodyAsText = request.bodyAsText;
    this.bodyAsFormUrlEncoded = request.bodyAsFormUrlEncoded;
    this.bodyAsJson = JSON.parse(request.bodyAsJsonString);
    this.method = request.method;
    this.uri = request.uri;
    this.contentType = request.contentType;
    this.secure = request.secure;
    this.headers = request.headers;
};

