

//eConn is the etherpad api connection
eConn = remote.connect("etherpad-api");

var sendUpdate = false;


padID = url.args.nodeRef.split("/").pop();

//Get the current User count.
res = eConn.get("padUsersCount?padID=" + padID);
userCount = eval('(' + res + ')');

//This will throw an exception if the API key is wrong.
if(userCount.data.padUsersCount == 0) {
    sendUpdate = true;
}

//If there are updates required, update the Pad with the current HTML
if(sendUpdate) {
    contentUrl = "/api/node/content/" + url.args.nodeRef.replace("://", "/");
    contentData = remote.call(contentUrl);

    //The Alfresco editor does not include <html> directives by default
    //Causes an etherpad error: TypeError: Cannot read property 'tagName' of undefined
    //Etherpad's SetPadHTML expects there to be at minimum a HTML directive.
    if(String(contentData).toLowerCase().indexOf("<html>") == -1) {
        contentData = "<html>" + contentData + "</html>";
    }
    //Create the pad, we don't really care if this fails.
    createPad = eConn.get("createPad?padID=" + padID);
    resetHTML = eConn.get("setHTML?padID=" + padID + "&html=" + encodeURIComponent(contentData));
}

model.nodeRef = padID;

model.userName = user.name;
