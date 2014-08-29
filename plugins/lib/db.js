exports.query = function () {
    return new Query();
};

exports.Collection = Collection;
exports.Schema = Schema;

importClass(Packages.beyond.engine.javascript.lib.database.ObjectId);
exports.ObjectId = function(value) {
    return new ObjectId(value);
};
