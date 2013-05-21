print("user.settings should not be an empty array, but an empty object");
db.user2.find({settings:{'$in':[{},[]]}}).forEach(function(user) {
  db.user2.update({'_id': user['_id']}, {'$unset':{settings:true}});
});

print("user.roles should not be an empty array, but an empty object");
db.user2.find({roles:{'$in':[{},[]]}}).forEach(function(user) {
  db.user2.update({'_id': user['_id']}, {'$unset':{roles:true}});
});

print("rename user.isChatBan -> user.troll");
db.user2.update({},{$rename:{isChatBan:'troll'}}, {multi:true});

print("add troll fields to the forum topics")
db.f_topic.find().forEach(function(topic) {
  db.f_topic.update({'_id': topic['_id']}, { $set: {
    troll: false,
    updatedAtTroll: topic['updatedAt'],
    nbPostsTroll: topic['nbPosts'],
    lastPostIdTroll: topic['lastPostId']
  }});
});

print("add troll fields to the forum categs")
db.f_categ.find().forEach(function(categ) {
  db.f_categ.update({'_id': categ['_id']}, { $set: {
    nbTopicsTroll: categ['nbTopics'],
    nbPostsTroll: categ['nbPosts'],
    lastPostIdTroll: categ['lastPostId']
  }});
});

print("remove useless author names in forum posts")
db.f_post.update({author:{$exists:true},userId:{$exists:true}},{$unset:{author:true}},{multi:true});

print("mark all forum posts as not troll");
db.f_post.update({},{$set:{troll:false}}, {multi:true});

print("use troll field in forum post indexes")
db.f_post.dropIndex('topicId_1')
db.f_post.dropIndex('topicId_1_createdAt_1')
db.f_post.dropIndex('categId_1')
db.f_post.dropIndex('createdAt_-1')
db.f_post.ensureIndex({topicId: 1, troll: 1})
db.f_post.ensureIndex({topicId: 1, createdAt: 1, troll: 1})
db.f_post.ensureIndex({categId: 1, troll: 1})
db.f_post.ensureIndex({createdAt: -1, troll: 1})

print("use troll field in forum topic indexes")
db.f_topic.dropIndex('categId_1')
db.f_topic.dropIndex('categId_1_updatedAt_-1')
db.f_topic.ensureIndex({categId: 1, troll: 1})
db.f_topic.ensureIndex({categId: 1, updatedAt: -1, troll: 1})

print("user.settings.{chat,sound} should be a string");
['settings.chat', 'settings.sound'].forEach(function(name) {
  [true, false].forEach(function(value) {
    var sel = {}
    sel[name] = value;
    printjson(sel);
    db.user2.find(sel).forEach(function(user) {
      var up = {}
      up[name] = value.toString();
      printjson(up);
      db.user2.update({'_id': user['_id']}, {'$set':up});
    });
  });
});

// print("Reset lobby_room");
// db.lobby_room.drop();
// db.createCollection("lobby_room",{capped:true,size:50000})

// print("Reset lobby_entry");
// db.lobby_entry.drop();
// db.createCollection("lobby_entry",{capped:true,size:10000})