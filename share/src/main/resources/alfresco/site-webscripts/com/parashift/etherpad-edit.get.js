

//eConn is the etherpad api connection
eConn = remote.connect("etherpad-api");

var sendUpdate = false;


padID = url.args.nodeRef.split("/").pop();

//Get the current User count.
res = eConn.get("padUsersCount?padID=" + padID);
userCount = JSON.parse(res);

//This will throw an exception if the API key is wrong.
if(userCount.data.padUsersCount == 0) {
    sendUpdate = true;
}

//If there are updates required, update the Pad with the current HTML
if(sendUpdate) {
    contentUrl = "/api/node/content/" + url.args.nodeRef.replace("://", "/");
    contentData = remote.call(contentUrl);
    //Create the pad, we don't really care if this fails.
    createPad = eConn.get("createPad?padID=" + padID);
    resetHTML = eConn.get("setHTML?padID=" + padID + "&html=" + encodeURIComponent(contentData));
}

model.nodeRef = padID;

model.userName = user.name;
