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
         beginHtml = htmlData.search("<body>")
         endHtml = htmlData.search("</body>");

         request.post(Alfresco.constants.PROXY_URI_RELATIVE + "api/node/" + nodeRef.replace("://", "/") + "/formprocessor", {
            headers: {
                "Content-Type" : "application/json",
            },
            data : JSON.stringify({
               prop_cm_content : htmlData.substring(beginHtml, endHtml + 7)
            })
         }).then(function() {
            Alfresco.util.PopupManager.displayMessage({"text": "Update Submitted"});
         });

    });

  });

  on(window, "resize", function() {
    resizeFrame(iframe);
  });

});


