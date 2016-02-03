<h1></h1>
<?php
if( !(isset($_POST['action']) && $_POST['action'] == 'save') )
	die("Invalid action");
	
$_id = $_POST['rid'];
$opts = get_option('ecapi_options');
$dbms = new couchdb(
	$opts['ecapi_config_db_url'], $opts['ecapi_config_db_name'], 
	$opts['ecapi_config_db_username'],$opts['ecapi_config_db_password']
);
$json = $dbms->getDoc($_id);
$json = $json['data'];
$_rev = $json->_rev;

if( !isset($_POST['_rev']) )
	die("Document revision ID not set in request. Cannot update.");
	
if( $_rev !== $_POST['_rev'] )
	die("Revision number mismatch! Expected '<span style=\"color:#f00\">{$_POST['_rev']}</span>', but latest is '<span style=\"color:#f00\">$_rev</span>'");

if( !isset($_POST ['debug']) ) unset( $json->debug );
foreach( $_POST as $a=>$v)
	switch( $a ) {
		case 'cache':
			$json->{"mks:cache-lifetime"} = $v;
			break;
		case 'debug':
			$json->debug = TRUE;
			break;
		case 'dataset-uuid':
			$json->{'catalogue-uuid'} = $v;
			break;
		case 'endpoint':
			$json->{"http://rdfs.org/ns/void#sparqlEndpoint"} = $v;
			break;
		case 'graph':
			$json->{"mks:graph"} = $v;
			break;
	}
$response = $dbms->saveDoc( $_id, $json );
if( $response['data']->ok == 1 )
	wp_redirect (  home_url() . '/wp-admin/admin.php?page=ecapi-config&action=view&id=' . urlencode($_id) );
else {
	die("FAIL:<pre>". print_r( $response,TRUE ). "</pre>");
}
