<@markup id="css" >
   <#-- CSS Dependencies -->
   <@link href="${url.context}/res/components/etherpad/etherpad.css" group="etherpad"/>
</@>

<@markup id="js">
   <#-- JavaScript Dependencies -->
   <@script src="${url.context}/res/components/etherpad/etherpad.js" group="etherpad"/>
</@>


<@markup id="html">
   <@uniqueIdDiv>
      <div class="etherpad-editor">
		<iframe id="editorFrame" src='/share/proxy/etherpad/p/${nodeRef}?userName=${user.name}&showChat=false'></iframe>
      </div>

      <#-- TODO: Move this into Dojo or YUI -->
      <div class="etherpad-save-back">
          <div class="form-buttons">
              <span class="yui-button yui-submit-button">
                  <span class="first-child">
                      <button type="button" id="etherpadSaveback">Save Back</button>
                  </span>
              </span>
          </div>
      </div>

   </@>
</@>