<div>
	<h2>Data API settings</h2>
	Customise how the Data Hub connects to APIs for providing access to their data.
	<form action="options.php" method="POST">
<?php settings_fields('ecapi_options'); ?>
<?php do_settings_sections('ecapi-settings'); ?>
		<input name="Submit" type="submit" value="<?php esc_attr_e('Save Changes'); ?>" />
	</form>
</div>