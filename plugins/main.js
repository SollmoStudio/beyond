var console = require("console");
var counter = require("counter");
var crypto = require("examples/crypto");
var db = require("examples/db");
var fs = require("fs");
var future = require("future");
var path = require("examples/path");
var Response = require("response").Response;
var uuid = require("examples/uuid");

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
        case "uuid":
            return new Response(uuid.test());
        case "successful":
            var f = future.successful(1);
            return f.map(function (v) { return new Response(v); });
        case "andThen":
            var f = future.successful(1);
            return f.andThen(function (result, isSuccess) {
              console.log("andThen1: " + result);
            }).andThen(function (result, isSuccess) {
              console.log("andThen2: " + result);
            }).map(function (v) { return new Response(v); });
        case "sequence":
            var f1 = future.successful(1);
            var f2 = future.successful(2);
            var f3 = future.successful(3);
            var f4 = future.successful(4);
            return Future.sequence(f1, f2, f3, f4).map(function (values) {
              var sum = values.reduce(function (acc, v) { return acc + v; });
              return new Response(sum);
            });
        case "firstCompletedOf":
            var f1 = future.successful(1);
            var f2 = future.successful(2);
            var f3 = future.successful(3);
            var f4 = future.successful(4);
            return Future.firstCompletedOf(f1, f2, f3, f4).map(function (value) {
                return new Response(value);
            });
        case "jsonRequest":
            return new Response(req.bodyAsJson);
        case "jsonResponse":
            var jsonMessage = {status: "ok", msg: "Hello World" };
            return new Response(jsonMessage);
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
            var obj = req.bodyAsFormUrlEncoded;
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
        case "referenceFindOne":
            db.referenceFindOne(tokens[0]);
            break;
        case "referenceInsert":
            db.referenceInsert(tokens[0]);
            break;
        case "referenceUpdate":
            db.referenceUpdate(tokens[0], tokens[1]);
            break;
        case "remove":
            db.remove.apply(db.remove, tokens);
            break;
        case "removeOne":
            db.removeOne.apply(db.remove, tokens);
            break;
        case "findOneWithKey":
            db.findOneWithKey(tokens[0]);
            break;
        case "embeddingInsert":
            db.embeddingInsert(tokens[0], tokens[1]);
            break;
        case "embeddingFind":
            db.embeddingFind.apply(db.embeddingFind, tokens);
            break;
        case "arrayInsert":
            db.arrayInsert.apply(db.arrayInsert, tokens);
            break;
        case "arrayFindOne":
            db.arrayFindOne(tokens[0]);
            break;
        case "save":
            var saveResult = db.save.apply(db.save, tokens);
            return new Response(saveResult);
        case "writeFile":
            var fileName = decodeURIComponent(tokens[0]);
            var content = req.bodyAsText;
            var result = fs.writeFile(fileName, content, {
                encoding: 'UTF-8',
                mode: 0755
            });
            return result.map(function () {
                return new Response(fileName + " written");
            });
        case "readFile":
            var fileName = decodeURIComponent(tokens[0]);
            var result = fs.readFile(fileName, {
                encoding: 'UTF-8'
            });
            return result.map(function (data) {
                return new Response(data);
            });
        case "readdir":
            var path = decodeURIComponent(tokens[0]);
            var result = fs.readdir(path);
            return result.map(function (files) {
                return new Response(files.map(function (file) {
                    var result = "[";
                    result += file.isFile ? "file " : file.isDirectory ? "dir " : "";
                    result += file.name + "]"
                    return result;
                }).toString());
            });
        case "path":
            return new Response(path.test());
        case "crypto":
            return new Response(crypto.test());
        default:
            break;
    }
    return new Response("Hello World");
};
