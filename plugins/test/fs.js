var assert = require('assert');
var fs = require('fs');
require('bdd').mount(this);

describe('file-system', function () {
  describe('#readFile()', function () {
    it.will('return the data of the file.', function (done) {
      fs
      .readFile('./plugins/test/assets/fs/beyond')
      .onComplete(function (data) {
        assert.async(done, function () {
          assert.equal(data, 'Beyond framework\n');
        });
      });
    });

    it.will('return the data of the file with a specific encoding.', function (done) {
      fs
      .readFile('./plugins/test/assets/fs/hello', {encoding: 'EUC-KR'})
      .onComplete(function (data) {
        assert.async(done, function () {
          assert.equal(data, '안녕, 세계!\n');
        });
      });
    });

    it.will('fire an exception when there is no file.', function (done) {
      fs
      .readFile('./plugins/test/assets/fs/nothing')
      .onComplete(function (result, isSuccess) {
        assert.async(done, function () {
          assert.equal(isSuccess, false);
          assert.equal(result, "Can't find the file");
        });
      });
    });
  });

  describe('#writeFile()', function () {
    var testFilePath = "./plugins/test/assets/fs/test";

    function exec(command) {
      return java.lang.Runtime.getRuntime().exec(command).waitFor();
    }

    function removeTestFile() {
      // Remove the file we used or will use
      exec("rm " + testFilePath);
    }
    beforeEach(removeTestFile);
    afterEach(removeTestFile);

    function contentEqualTo(expected, done, options) {
      var future;
      if (typeof options === 'undefined') {
        future = fs.readFile(testFilePath);
      } else {
        future = fs.readFile(testFilePath, options);
      }

      future
      .onComplete(function (data) {
        assert.async(done, function () {
          assert.equal(data, expected);
        });
      });
    }

    function permissionEqualTo(expected, done) {
      assert.async(done, function () {
        var error = !!exec(["sh", "-c", "find " + testFilePath + " -perm " + expected + " | grep '.*'"]);
        assert.equal(error, false);
      });
    }

    it.will('write data to a file.', function (done) {
      fs
      .writeFile(testFilePath, "Hello, world!")
      .onComplete(function () {
        contentEqualTo("Hello, world!", done);
      });
    });

    it.will('write data to a file with encoding.', function (done) {
      fs
      .writeFile(testFilePath, "안녕, 세계!", {encoding: 'EUC-KR'})
      .onComplete(function () {
        contentEqualTo("안녕, 세계!", done, {encoding: 'EUC-KR'});
      });
    });

    it.will('write data to a file with mode.', function (done) {
      fs
      .writeFile(testFilePath, "Hello, world!", {mode: 0765})
      .onComplete(function () {
        permissionEqualTo('765', done);
      });
    });

    it.will('write data to a file with encoding and mode.', function (done) {
      fs
      .writeFile(testFilePath, "안녕, 세계!", {encoding: 'EUC-KR', mode: 0657})
      .onComplete(function () {
        permissionEqualTo('657', function (err) {
          if (err) {
            done(err);
          } else {
            contentEqualTo("안녕, 세계!", done, {encoding: 'EUC-KR'});
          }
        });
      });
    });
  });

  describe('#readdir()', function () {
    var future;
    beforeEach(function () {
      future = fs.readdir('./plugins/test/assets/fs');
    });

    function findWithName(files, name) {
      return files.filter(function (file) {
        return file.name === name;
      })[0];
    }

    it.will('return the list of files with the correct length.', function (done) {
      future
      .onComplete(function (files) {
        assert.async(done, function () {
          assert.equal(files.length, 3);
        });
      });
    });

    it.will('return the list of files containing static files', function (done) {
      future
      .onComplete(function (files) {
        assert.async(done, function () {
          assert.equal(findWithName(files, 'hello').name, 'hello');
          assert.equal(findWithName(files, 'hello').path, './plugins/test/assets/fs/hello');
          assert.equal(findWithName(files, 'hello').isFile, true);
          assert.equal(findWithName(files, 'hello').isDirectory, false);

          assert.equal(findWithName(files, 'beyond').name, 'beyond');
          assert.equal(findWithName(files, 'beyond').path, './plugins/test/assets/fs/beyond');
          assert.equal(findWithName(files, 'beyond').isFile, true);
          assert.equal(findWithName(files, 'beyond').isDirectory, false);
        });
      });
    });

    it.will('return the list of files containing the directory', function (done) {
      future
      .onComplete(function (files) {
        assert.async(done, function () {
          assert.equal(findWithName(files, 'dir').name, 'dir');
          assert.equal(findWithName(files, 'dir').path, './plugins/test/assets/fs/dir');
          assert.equal(findWithName(files, 'dir').isFile, false);
          assert.equal(findWithName(files, 'dir').isDirectory, true);
        });
      });
    });
  });
});
