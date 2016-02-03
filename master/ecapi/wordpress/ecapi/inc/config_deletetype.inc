<h1></h1>
<?php
if( !(isset($_POST['action']) && $_POST['action'] == 'deletetype') )
	die("Invalid action");
	
$_id = $_POST['id'];
$typeid = $_POST['typeid'];
$opts = get_option('ecapi_options');
$dbms = new couchdb(
	$opts['ecapi_config_db_url'], $opts['ecapi_config_db_name'], 
	$opts['ecapi_config_db_username'], $opts['ecapi_config_db_password']
);
$json = $dbms->getDoc($_id);
$json = $json['data'];
$_rev = $json->_rev;

if( !isset($_POST['_rev']) )
	die("Document revision ID not set in request. Cannot update.");
	
if( $_rev !== $_POST['_rev'] )
	die("Revision number mismatch! Expected '<span style=\"color:#f00\">{$_POST['_rev']}</span>', but latest is '<span style=\"color:#f00\">$_rev</span>'");

if( !isset($json->{"mks:types"}->$typeid) )
	die("Supplied type ID is not registered with this configuration. Cannot delete.");

unset($json->{"mks:types"}->$typeid);

$response = $dbms->saveDoc( $_id, $json );
if( $response['data']->ok == 1 )
	wp_redirect (  home_url() . '/wp-admin/admin.php?page=ecapi-config&action=view&id=' . urlencode($_id) );
else {
	die("FAIL:<pre>". print_r( $response,TRUE ). "</pre>");
}
