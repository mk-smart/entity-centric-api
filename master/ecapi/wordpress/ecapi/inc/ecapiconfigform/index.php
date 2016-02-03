<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>ECAPI Config Form</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" integrity="sha512-dTfge/zgoMYpP7QbHy4gWMEGsbsdZeCXz7irItjcC3sPUFtf0kuFbDz/ixG7ArTxmDjLXDmezHubeNikyKGVyQ==" crossorigin="anonymous"> 
    <style>
    #main {
       margin: 10px 10% 10px 10%; 
    }
    </style>
  </head>  
  <body>

  <div id="main">
    
    <?php 
       function startswith($haystack, $needle) {
           return substr($haystack, 0, strlen($needle)) === $needle;
       }
    ?>


   <?php if (!isset($_POST['rid'])) { ?>
       <form action="index.php" method="POST">
            <input type="text" placeholder="config doc id" size="100%" name="rid"  id="rid"><br/>
            <input type="text" placeholder="couchdb url" size="100%" name="cburl"  id="cburl"><br/>
            <input type="text" placeholder="couchdb DB"  size="100%" name="cbdb"   id="cbdb"><br/>
            <input type="text" placeholder="couchdb user"  size="100%" name="cbuser" id="cbuser"><br/>
            <input type="password" placeholder="couchdb password" size="100%" name="pw"     id="pw"><br/>
	    <input type="submit" value="retrieve" />
       </form>
   <?php } else { 
       require_once('couchdb.class.php');
       $_id = $_POST['rid'];
       $cburl = "https://data.beta.mksmart.org/config";
       if (isset($_POST['cburl'])) $cburl = $_POST['cburl'];
       $cbdb  = 'mksmart_ecapi';
       if (isset($_POST['cbdb'])) $cbdb = $_POST['cbdb'];
       $cbuser = '';
       if (isset($_POST['cbuser'])) $cbuser = $_POST['cbuser'];
       $pw = '';
       if (isset($_POST['pw'])) $pw = $_POST['pw'];
       $cb = new couchdb($cburl, $cbdb, $cbuser, $pw);
       $docjson = $cb->getDoc($_id);
//       print_r($docjson);
       ?> 
   <?php 
      // saving
      if (isset($_POST['save']) && strcmp($_POST['save'], 'save')===0){
      $jsons = '{"_id": "'.$_id.'", "type": "provider-spec"';      
      $types = array();
      foreach($_POST as $a=>$v){
         if (strcmp($v, '')!==0) {
             if (strcmp($a, 'cache')===0){
                 $jsons .= ', "mks:cache-lifetime": '.$v;
             } else if (strcmp($a, 'debug')===0){
                 if (strcmp($v, "debug")===0) $jsons .= ', "debug": true';
             } else if (strcmp($a, 'endpoint')===0){
                 $jsons .= ', "http://rdfs.org/ns/void#sparqlEndpoint": "'.$v.'"';
             } else if (strcmp($a, 'graph')===0){
                 $jsons .= ', "mks:graph": "'.$v.'"';
             } else if (strcmp($a, '_rev')===0){
                 $jsons .= ', "_rev": "'.$v.'"';
             } else if (startswith($a, "localise__")){
                 $type = substr($a, strpos($a, '__')+2);
                 $type = urldecode(urldecode($type));
                 if (strcmp($type, "newtype")===0) {
                      if (strcmp($_POST['name__newtype'], '')!==0){
                         if (!isset($types[$_POST['name__newtype']])) $types[$_POST['name__newtype']] = array();
                         $types[$_POST['name__newtype']]['localise'] = $v; // replace newline by double space
                      }
                 } else {
                     if (!isset($types[$type])) $types[$type] = array();
                     $types[$type]['localise'] = $v; // replace newline by double space
                 }
             } else if (startswith($a, "query__")){
                 $type = substr($a, strpos($a, '__')+2);
                 $type = urldecode(urldecode($type));
                 if (strcmp($type, "newtype")===0) {
                      if (strcmp($_POST['name__newtype'], '')!==0){
                         if (!isset($types[$_POST['name__newtype']])) $types[$_POST['name__newtype']] = array();
                         $types[$_POST['name__newtype']]['query_text'] = $v; // replace newline by double space
                      }
                 } else {
                     if (!isset($types[$type])) $types[$type] = array();
                     $types[$type]['query_text'] = $v; // replace newline by double space
                 }
             } else if (startswith($a, "fetch__")){
                 $type = substr($a, strpos($a, '__')+2);
                 $type = urldecode(urldecode($type));
                 if (strcmp($type, "newtype")===0) {
                      if (strcmp($_POST['name__newtype'], '')!==0){
                         if (!isset($types[$_POST['name__newtype']])) $types[$_POST['name__newtype']] = array();
                         $types[$_POST['name__newtype']]['fetch_query'] = $v; // replace newline by double space
                      }
                 } else {
                     if (!isset($types[$type])) $types[$type] = array();
                     $types[$type]['fetch_query'] = $v; // replace newline by double space
                 }
             }
             else {
                // echo $a.'='.$v.'<br/>';
             }
          }
      }
      $jsons.=', "mks:types": '.json_encode($types);
      $jsons.='}';
      echo $jsons.'<br/>';
      $r = $cb->saveDoc($_id, $jsons);
      echo '<pre>'.$r.'</pre>';
   }
   ?>
       <form action="index.php" method="POST">
           <input type="text" placeholder="config doc id" size="100%" name="rid" id="rid" value="<?php echo $_id;?>"><br/>
           <?php if (isset($docjson->_rev)) { ?>
	       <input type="hidden" name="_rev" value="<?php echo $docjson->_rev; ?>" /> 
           <?php } ?>
            <input type="text" placeholder="couchdb url" size="100%" name="cburl"  id="cburl" value="<?php echo $cburl; ?>"><br/>
            <input type="text" placeholder="couchdb DB"  size="100%" name="cbdb"   id="cbdb" value="<?php echo $cbdb; ?>"><br/>
            <input type="text" placeholder="couchdb user"  size="100%" name="cbuser" id="cbuser" value="<?php echo $cbuser; ?>"><br/>
            <input type="password" placeholder="couchdb password" size="100%" name="pw" id="pw" value="<?php echo $pw; ?>"><br/><br/>
	    <?php if (isset($docjson->debug) && $docjson->debug==true) { ?>
	    	    Debug: <input type="checkbox" name="debug" value="debug" id="debug" checked="checked"/><br/>
	    <?php } else  { ?>
	    Debug: <input type="checkbox" name="debug" value="debug" id="debug"/><br/>
	    <?php } ?>
            <input type="text" placeholder="cachetime"  size=30 name="cache" id="cache" value="<?php echo $docjson->{"mks:cache-lifetime"}; ?>"><br/><br/>
            <input type="text" placeholder="sparql endpoint"  size="100%" name="endpoint" id="endpoint" value="<?php echo $docjson->{"http://rdfs.org/ns/void#sparqlEndpoint"}; ?>"><br/>
            <input type="text" placeholder="RDF graph"  size="100%" name="graph" id="graph" value="<?php echo $docjson->{"mks:graph"}; ?>"><br/><br/>
	    <div id="typelist">
	    
	      <?php foreach($docjson->{"mks:types"} as $t=>$a) { ?>
	       <div class="typedesc" id="type_<?php echo urlencode($t); ?>">
	          <h3>Type: <?php echo $t; ?></h3>
		  localise:<br/>
		  <!-- TODO: add template button -->
		  <textarea name="localise__<?php echo urlencode($t); ?>" id="localise__<?php echo urlencode($t); ?>" cols="100%" rows=6 ><?php echo $a->localise; ?></textarea></br/>
		  query text:</br/>
		  <textarea name="query__<?php echo urlencode($t); ?>" id="query__<?php echo urlencode($t); ?>" cols="100%" rows=6 ><?php echo $a->query_text; ?></textarea></br/>
		  fetch query:<br/>
		  <textarea name="fetch__<?php echo urlencode($t); ?>" id="fetch__<?php echo urlencode($t); ?>" cols="100%" rows=6 ><?php echo $a->fetch_query; ?></textarea></br/>
	       </div>
	       <?php } ?>
	       <div class="typedesc" id="type__newtype">
	          <h3>New Type</h3>
		  name:<br/>
		  <input type="text" name="name__newtype" id="name__newtype" size="100%" /></br/>
		  localise:<br/>
		  <textarea name="localise__newtype" id="localise__newtype" cols="100%" rows=6 ></textarea></br/>
		  query text:</br/>
		  <textarea name="query__newtype" id="query__newtype" cols="100%" rows=6 ></textarea></br/>
		  fetch query:<br/>
		  <textarea name="fetch__newtype" id="fetch__newtype" cols="100%" rows=6 ></textarea></br/>
	       </div>
            </div>
	    <input type="submit" name="reset" value="reset" />
	    <input type="submit" name="save" value="save" />
       </form>
   <?php } ?>
	
	</div>

    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js" integrity="sha512-K1qjQ+NcF2TYO/eI3M6v8EiNYZfA95pQumfvcVrTHtwQVDG+aHRqLi/ETn2uB+1JqwYqVG3LIvdm9lj6imS/pQ==" crossorigin="anonymous"></script>    

  </body>
</html>
