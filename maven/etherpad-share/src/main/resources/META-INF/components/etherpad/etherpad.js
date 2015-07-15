require(["dojo/dom", "dojo/query", "dojo/on", "dojo/request", "dojo/domReady!"], function(dom, query, on, request){

  function resizeFrame(elem) {
      elem.style.height = (window.innerHeight - 250) + "px";
  }


  function getURLParameter(name) {
        return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search) || [, ""])[1].replace(/\+/g, '%20')) || null
  }

  var iframe = dom.byId("editorFrame");
  var saveBack = dom.byId("etherpadSaveback");

  var nodeRef = getURLParameter("nodeRef");

  resizeFrame(iframe);

  on(saveBack, "click", function() {

    padID = nodeRef.split("/").pop();

    request.get(Alfresco.constants.URL_CONTEXT + "proxy/etherpad/p/" + padID + "/export/html").then(function(htmlData) {

         //This is to remove any CSS and title elements and just include the body of the HTML
         //Regex doesn't seem to work
         var beginHtml = htmlData.search("<body>")
         var endHtml = htmlData.search("</body>");

         var htmlSubmission = "<html>" + htmlData.substring(beginHtml, endHtml + 7) + "</html>";

         var postHeaders = {
            "Content-Type" : "application/json"
         }

         if (Alfresco.util.CSRFPolicy && Alfresco.util.CSRFPolicy.isFilterEnabled()) {
            postHeaders[Alfresco.util.CSRFPolicy.getHeader()] = Alfresco.util.CSRFPolicy.getToken();
         }

         request.post(Alfresco.constants.PROXY_URI_RELATIVE + "api/node/" + nodeRef.replace("://", "/") + "/formprocessor", {
            headers: postHeaders,
            data : JSON.stringify({
               prop_cm_content : htmlSubmission
            })
         }).then(function() {
            Alfresco.util.PopupManager.displayMessage({"text": "Update Submitted"});
         }, function (err) {
            //Tries to find the exception from the error page and send a prompt
            beginException = err.response.data.search("<!--") + 4;
            endException = err.response.data.search("-->");
            Alfresco.util.PopupManager.displayPrompt({"title": "Error Updating Alfresco",
                                                      "text" : err.message + "\n " + err.response.data.substring(beginException, endException)});
         });

    }, function(err) {
      Alfresco.util.PopupManager.displayPrompt({"title": "Error Getting HTML",
                                          "text" : err.message});

    });

  });

  on(window, "resize", function() {
    resizeFrame(iframe);
  });

});


