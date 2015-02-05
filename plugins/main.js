var buffer = require("examples/buffer");
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
  var f, f1, f2, f3, f4, obj, prop, fileName, result, sum;

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
      f = future.successful(1);
      return f.map(function (v) { return new Response(v); });
    case "andThen":
      f = future.successful(1);
      return f.andThen(function (result) {
        console.log("andThen1: " + result);
      }).andThen(function (result) {
        console.log("andThen2: " + result);
      }).map(function (v) { return new Response(v); });
    case "sequence":
      f1 = future.successful(1);
      f2 = future.successful(2);
      f3 = future.successful(3);
      f4 = future.successful(4);
      return Future.sequence(f1, f2, f3, f4).map(function (values) {
        sum = values.reduce(function (acc, v) { return acc + v; });
        return new Response(sum);
      });
    case "firstCompletedOf":
      f1 = future.successful(1);
      f2 = future.successful(2);
      f3 = future.successful(3);
      f4 = future.successful(4);
      return Future.firstCompletedOf(f1, f2, f3, f4).map(function (value) {
        return new Response(value);
      });
    case "jsonRequest":
      return new Response(req.bodyAsJson);
    case "jsonResponse":
      result = {status: "ok", msg: "Hello World" };
      return new Response(result);
    case "contentType":
      return new Response(req.contentType);
    case "secure":
      return new Response(req.secure.toString());
    case "headers":
      result = "";
      obj = req.headers;
      for (prop in obj) {
        // FIXME: obj does not support hasOwnProperty().
        result += prop + " = " + obj[prop] + ";";
      }
      return new Response(result);
    case "post":
      return new Response("body = " + req.bodyAsText);
    case "postFormUrlEncoded":
      result = "";
      obj = req.bodyAsFormUrlEncoded;
      for (prop in obj) {
        result += prop + " = " + obj[prop] + ";";
      }
      return new Response("body = " + result);
    case "insert":
      db.insert(tokens[0], tokens[1]);
      break;
    case "find":
      db.find.apply(db.find, tokens);
      break;
    case "findOne":
      db.findOne.apply(db.findOne, tokens);
      break;
    case "count":
      db.count.apply(db.count, tokens);
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
      result = db.save.apply(db.save, tokens);
      return new Response(result);
    case "writeFile":
      fileName = decodeURIComponent(tokens[0]);
      var content = req.bodyAsText;
      result = fs.writeFile(fileName, content, {
        encoding: 'UTF-8',
        mode: 0755
      });
      return result.map(function () {
        return new Response(fileName + " written");
      });
    case "readFile":
      fileName = decodeURIComponent(tokens[0]);
      result = fs.readFile(fileName, {
        encoding: 'UTF-8'
      });
      return result.map(function (data) {
        return new Response(data);
      });
    case "readdir":
      fileName = decodeURIComponent(tokens[0]);
      result = fs.readdir(fileName);
      return result.map(function (files) {
        return new Response(files.map(function (file) {
          result = "[";
          result += file.isFile ? "file " : file.isDirectory ? "dir " : "";
          result += file.name + "]";
          return result;
        }).toString());
      });
    case "path":
      return new Response(path.test());
    case "crypto":
      return new Response(crypto.test());
    case "buffer":
      return new Response(buffer.test());
    case "static":
      var staticPath = './plugins/test/assets/fs/';
      fileName = decodeURIComponent(tokens[0]);
      return new Response(fs.read(staticPath + fileName));
    default:
      break;
  }
  return new Response("Hello World");
};
