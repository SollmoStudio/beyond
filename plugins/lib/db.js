exports.query = function () {
    return new Query();
};

exports.Collection = Collection;
exports.Schema = Schema;

importClass(Packages.beyond.engine.javascript.lib.database.ObjectID);
exports.ObjectID = function(value) {
    return new ObjectID(value);
};
