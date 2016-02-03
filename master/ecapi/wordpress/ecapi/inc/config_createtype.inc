<h1></h1>
<?php
if( !(isset($_POST['action']) && $_POST['action'] == 'createtype') )
	die("Invalid action");
	
$_id = $_POST['id'];
$typeid = $_POST['typeid'];
$opts = get_option('ecapi_options');
$dbms = new couchdb(
	$opts['ecapi_config_db_url'], $opts['ecapi_config_db_name'], 
	$opts['ecapi_config_db_username'], $opts['ecapi_config_db_password']
);
$json = $dbms->getDoc($_id);
print "<pre>";
print_r($json);
print "</pre>";

$json = $json['data'];

$_rev = $json->_rev;

if( !isset($_POST['_rev']) )
	die("Document revision ID not set in request. Cannot update.");
	
if( $_rev !== $_POST['_rev'] )
	die("Revision number mismatch! Expected '<span style=\"color:#f00\">{$_POST['_rev']}</span>', but latest is '<span style=\"color:#f00\">$_rev</span>'");

if( isset($json->{"mks:types"}->$typeid) )
	die("Supplied type ID is already registered with this configuration. Use 'Edit' to change its configuration.");

foreach( $_POST as $a => $v)
	switch( $a ) {
		case 'localise':
			if( !empty($v) ) $json->{"mks:types"}->$typeid->localise = $v;
			break;
		case 'query_text':
			if( !empty($v) ) $json->{"mks:types"}->$typeid->query_text = $v;
			break;
		case 'fetch_query':
			if( !empty($v) ) $json->{"mks:types"}->$typeid->fetch_query = $v;
			break;
	}
$response = $dbms->saveDoc( $_id, $json );
if( $response['data']->ok == 1 )
	wp_redirect (  home_url() . '/wp-admin/admin.php?page=ecapi-config&action=view&id=' . urlencode($_id) );
else {
	die("FAIL:<pre>". print_r( $response,TRUE ). "</pre>");
}
