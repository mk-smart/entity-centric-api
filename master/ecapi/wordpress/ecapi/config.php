<?php
if($_SERVER['REQUEST_METHOD'] == 'POST'){
	switch($_POST['action']) {
		case 'createtype':
			include_once 'inc/config_createtype.inc';
			break;
		case 'deletetype':
			include_once 'inc/config_deletetype.inc';
			break;
		case 'save':
			include_once 'inc/config_editsave.inc';
			break;
		case 'savetype':
			include_once 'inc/config_editsavetype.inc';
			break;
	}
}

else switch($_GET['action']) {
	case 'confirm_deletetype':
		include 'inc/config_confirm_deletetype.phtml';
		break;
	case 'addtype':
		include 'inc/config_addtype.phtml';
		break;
	case 'edittype':
		include 'inc/config_edittype.phtml';
		break;
	case 'edit':
		if( !isset($_GET['id']) ) die('No id parameter, nothing to view.');
		include 'inc/config_edit.phtml';
		break;
	case 'view':
		if( !isset($_GET['id']) ) die('No id parameter, nothing to view.');
		include 'inc/config_view.phtml';
		break;
	case 'list':
	default:
		include 'inc/config_list.phtml';
}