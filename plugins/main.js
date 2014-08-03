var counter = require("counter");
var db = require("examples/db");
var future = require("future");
var response = require("response");

exports.handle = function (req) {
    var tokens = req.uri.split("/");
    tokens.shift();
    tokens.shift();
    var operator = tokens.shift();
    switch (operator) {
        case "counter":
            return new Response('Hello ' + req.uri + ' ' + counter.count());
        case "futureCounter":
            return future.create(function () {
                return new Response('Hello future ' + req.uri + ' ' + counter.count());
            });
        case "jsonRequest":
            return new Response(req.bodyAsJsonString);
        case "jsonResponse":
            var jsonMessage = {status: "ok", msg: "Hello World" };
            return response.create(jsonMessage);
        case "contentType":
            return new Response(req.contentType);
        case "secure":
            return new Response(req.secure.toString());
        case "headers":
            var headers = "";
            var obj = req.headers;
            for (var prop in obj) {
                // FIXME: obj does not support hasOwnProperty().
                headers += prop + " = " + obj[prop] + ";";
            }
            return new Response(headers);
        case "post":
            return new Response("body = " + req.bodyAsText);
        case "postFormUrlEncoded":
            var body = "";
            var obj = req.bodyAsFormUrlEncoded
            for (var prop in obj) {
                body += prop + " = " + obj[prop] + ";";
            }
            return new Response("body = " + body);
        case "insert":
            db.insert(tokens[0], tokens[1]);
            break;
        case "find":
            db.find.apply(db.find, tokens);
            break;
        case "findOne":
            db.findOne.apply(db.findOne, tokens);
            break;
        case "remove":
            db.remove.apply(db.remove, tokens);
            break;
        case "removeOne":
            db.removeOne.apply(db.remove, tokens);
            break;
        default:
            break;
    }
    return new Response("Hello World");
}
