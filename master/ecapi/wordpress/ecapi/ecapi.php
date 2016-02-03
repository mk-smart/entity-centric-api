<?php
/*
Plugin Name: Data API management
Plugin URI: http://kmi.open.ac.uk/people/member/alessandro-adamou
Version: 0.0.2
Author: Alessandro Adamou
Author URI: http://kmi.open.ac.uk/people/member/alessandro-adamou
*/

class DataApis
{
    /**
     * A Unique Identifier
     */
    protected $plugin_slug;
    
    /**
     * A reference to an instance of this class.
     */
    private static $instance;
    
    /**
     * The array of templates that this plugin tracks.
     */
    protected $templates;
    
    /**
     * Returns an instance of this class.
     */
    public static function get_instance() {
        if (null == self::$instance)
            self::$instance = new DataApis();
        return self::$instance;
    }
    
    /**
     * Initializes the plugin by setting filters and administration functions.
     */
    private function __construct() {
        $this->templates = array();
        
        add_filter('body_class', array(
            $this,
            'register_body_classes'
        ));
        
        // Add a filter to the attributes metabox to inject template into the cache.
        add_filter('page_attributes_dropdown_pages_args', array(
            $this,
            'register_project_templates'
        ));
        // Add a filter to the save post to inject out template into the page cache
        add_filter('wp_insert_post_data', array(
            $this,
            'register_project_templates'
        ));
        // Add a filter to the template include to determine if the page has our
        // template assigned and return it's path
        add_filter('template_include', array(
            $this,
            'view_project_template'
        ));
        // Add your templates to this array.
        $this->templates = array(
            'inc/ecapi-root.phtml' => 'Data API root'
        );
    }
    
    public function register_body_classes($classes) {
        $classes[] = 'custom-background';
        $classes[] = 'content-onecolumn';
        return $classes;
    }
    
    
    /**
     * Adds our template to the pages cache in order to trick WordPress
     * into thinking the template file exists where it doens't really exist.
     *
     */
    public function register_project_templates($atts) {
        // Create the key used for the themes cache
        $cache_key = 'page_templates-' . md5(get_theme_root() . '/' . get_stylesheet());
        
        // Retrieve the cache list.
        // If it doesn't exist, or it's empty prepare an array
        $templates = wp_get_theme()->get_page_templates();
        if (empty($templates))
            $templates = array();
        
        // New cache, therefore remove the old one
        wp_cache_delete($cache_key, 'themes');
        
        // Now add our template to the list of templates by merging our templates
        // with the existing templates array from the cache.
        $templates = array_merge($templates, $this->templates);
        
        // Add the modified cache to allow WordPress to pick it up for listing
        // available templates
        wp_cache_add($cache_key, $templates, 'themes', 1800);
        
        return $atts;
    }
    
    /**
     * Checks if the template is assigned to the page
     */
    public function view_project_template($template) {
        global $post;
        
        if( !isset($post->ID) 
			|| !isset($this->templates[get_post_meta($post->ID, '_wp_page_template', TRUE)]) )
            return $template;
        
        $file = plugin_dir_path(__FILE__) . get_post_meta($post->ID, '_wp_page_template', TRUE);
        
        // Just to be safe, we check if the file exist first
        if (file_exists($file))
            return $file;
        else
            echo $file;
        
        return $template;
    }
    
}


/*************************
 ******* CALLBACKS *******
 *************************/

function ecapi_admin_add_page_settings() {
	add_menu_page('Entity API', 'Entity API', 'manage_options', 'ecapi-config', 'ecapi_config_page');
    add_options_page('Data API settings', 'Data API', 'manage_options', 'ecapi-settings', 'ecapi_options_page');
}

function ecapi_admin_init() {
    register_setting('ecapi_options', 'ecapi_options', 'ecapi_options_validate');
    add_settings_section('ecapi_main', 'Providers', 'ecapi_section_text', 'ecapi-settings');
    add_settings_field('ecapi_text_url', 'Entity-centric API root URL', 'ecapi_setting_url', 'ecapi-settings', 'ecapi_main');
    add_settings_field('ecapi_text_swagger', 'Swagger manifest URL', 'ecapi_setting_swagger', 'ecapi-settings', 'ecapi_main');
    add_settings_field('ecapi_config_db_url', 'CouchDB URL', 'ecapi_setting_config_db_url', 'ecapi-settings', 'ecapi_main');
    add_settings_field('ecapi_config_db_name', 'CouchDB configuration database name', 'ecapi_setting_config_db_name', 'ecapi-settings', 'ecapi_main');
    add_settings_field('ecapi_config_db_username', 'CouchDB username', 'ecapi_setting_config_db_username', 'ecapi-settings', 'ecapi_main');
    add_settings_field('ecapi_config_db_password', 'CouchDB password', 'ecapi_setting_config_db_password', 'ecapi-settings', 'ecapi_main');
}

function ecapi_config_page() {
    require_once dirname(__FILE__) . '/inc/ecapiconfigform/couchdb.class.php';
	include dirname(__FILE__) . '/config.php';
}

function ecapi_options_page() {
    include dirname(__FILE__) . '/options.php';
}

function ecapi_options_validate($input) {
    $options                  = get_option('ecapi_options');
    $options['ecapi_url']     = trim($input['ecapi_url']);
    $options['ecapi_swagger'] = trim($input['ecapi_swagger']);
    $options['ecapi_config_db_url'] = trim($input['ecapi_config_db_url']);
    $options['ecapi_config_db_name'] = trim($input['ecapi_config_db_name']);
    $options['ecapi_config_db_username'] = trim($input['ecapi_config_db_username']);
    $options['ecapi_config_db_password'] = $input['ecapi_config_db_password'];
    $url_rgx                  = '|^(http(s)?:)?//[a-z0-9-]+(.[a-z0-9-]+)*(:[0-9]+)?(/.*)?$|i';
    if (!preg_match($url_rgx, $options['ecapi_url']))
        $options['ecapi_url'] = '';
    if (!preg_match($url_rgx, $options['ecapi_swagger']))
        $options['ecapi_swagger'] = '';
    if (!preg_match($url_rgx, $options['ecapi_config_db']))
        $options['ecapi_config_db'] = '';
    return $options;
}

function ecapi_section_text() {
    print '<p>Configure which data providers the Data Hub portal will reach for.</p>';
}

function ecapi_setting_config_db_url() {
    $options = get_option('ecapi_options');
    print "<input id=\"ecapi_config_db_url\" name=\"ecapi_options[ecapi_config_db_url]\" size=\"48\" type=\"text\" value=\"{$options['ecapi_config_db_url']}\"/>";
}

function ecapi_setting_config_db_name() {
    $options = get_option('ecapi_options');
    print "<input id=\"ecapi_config_db_name\" name=\"ecapi_options[ecapi_config_db_name]\" size=\"32\" type=\"text\" value=\"{$options['ecapi_config_db_name']}\"/>";
}

function ecapi_setting_config_db_username() {
    $options = get_option('ecapi_options');
    print "<input id=\"ecapi_config_db_username\" name=\"ecapi_options[ecapi_config_db_username]\" size=\"32\" type=\"text\" value=\"{$options['ecapi_config_db_username']}\"/>";
}

function ecapi_setting_config_db_password() {
    $options = get_option('ecapi_options');
    print "<input id=\"ecapi_config_db_password\" name=\"ecapi_options[ecapi_config_db_password]\" size=\"32\" type=\"password\" value=\"{$options['ecapi_config_db_password']}\"/>";
}

function ecapi_setting_swagger() {
    $options = get_option('ecapi_options');
    print "<input id=\"ecapi_text_swagger\" name=\"ecapi_options[ecapi_swagger]\" size=\"48\" type=\"text\" value=\"{$options['ecapi_swagger']}\"/>";
}

function ecapi_setting_url() {
    $options = get_option('ecapi_options');
    print "<input id=\"ecapi_text_url\" name=\"ecapi_options[ecapi_url]\" size=\"48\" type=\"text\" value=\"{$options['ecapi_url']}\"/>";
}

/**
 * Loads necessary scripts
 */
function wptuts_scripts_with_the_lot() {
    $plugdir = dirname(__FILE__) . '/ecapi'; // Stupid Wordpress function plugins_url going bananas
    wp_register_script('shred', plugins_url('lib/swagger-ui/lib/shred.bundle.js', $plugdir), FALSE);
    wp_deregister_script('jquery'); // Foggin' noConflict crap
    wp_register_script('jquery', plugins_url('lib/jquery/jquery-1.10.2.min.js', $plugdir), FALSE);
	
    // typeahead.js
	wp_register_script('typeahead.js', plugins_url('lib/typeahead.js/typeahead.bundle.min.js', $plugdir), array(
        'jquery'
    ));	
	wp_register_script('jquery-blockui', plugins_url('lib/jquery/jquery.blockUI-2.70.0.min.js', $plugdir), array(
        'jquery'
    ));
	
    // jQuery dependencies for Swagger
    wp_register_script('jquery-slideto', plugins_url('lib/swagger-ui/lib/jquery.slideto.min.js', $plugdir), array(
        'jquery'
    ));
    wp_register_script('jquery-wiggle', plugins_url('lib/swagger-ui/lib/jquery.wiggle.min.js', $plugdir), array(
        'jquery'
    ));
    wp_register_script('jquery-ba-bbq', plugins_url('lib/swagger-ui/lib/jquery.ba-bbq.min.js', $plugdir), array(
        'jquery'
    ));
    wp_register_script('swagger', plugins_url('lib/swagger-ui/lib/swagger.js', $plugdir), array(
        'jquery'
    ), '2.0.47');
    wp_register_script('handlebars', plugins_url('lib/swagger-ui/lib/handlebars-1.0.0.js', $plugdir), array(
        'jquery'
    ));
    // Underscore and Backbone need to be in the head.
    wp_deregister_script('underscore');
    wp_register_script('underscore', plugins_url('lib/swagger-ui/lib/underscore-min.js', $plugdir), FALSE);
    wp_deregister_script('backbone');
    wp_register_script('backbone', plugins_url('lib/swagger-ui/lib/backbone-min.js', $plugdir), FALSE);
    // Swagger tax
    wp_register_script('swagger-client', plugins_url('lib/swagger-ui/lib/swagger-client.js', $plugdir), array(
        'swagger'
    ));
    wp_register_script('swagger-ui', plugins_url('lib/swagger-ui/swagger-ui.min.js', $plugdir), array(
        'swagger'
    ));
    wp_register_script('highlight', plugins_url('lib/swagger-ui/lib/highlight.7.3.pack.js', $plugdir), array(
        'jquery'
    ));
    
    wp_enqueue_script('shred');
    wp_enqueue_script('jquery');
    wp_enqueue_script('jquery-blockui');
	wp_enqueue_script('typeahead.js');
    wp_enqueue_script('jquery-slideto');
    wp_enqueue_script('jquery-wiggle');
    wp_enqueue_script('jquery-ba-bbq');
    wp_enqueue_script('underscore');
    wp_enqueue_script('backbone');
    wp_enqueue_script('handlebars');
    wp_enqueue_script('swagger');
    wp_enqueue_script('swagger-client');
    wp_enqueue_script('swagger-ui');
    wp_enqueue_script('highlight');
}

/**
 * Loads necessary stylesheets
 */
function wptuts_styles_with_the_lot() {
    $plugdir = dirname(__FILE__) . '/ecapi'; // Stupid Wordpress function plugins_url going bananas
    // Swagger and custom styles
    // wp_register_style('swaggerui-reset', plugins_url('lib/swagger-ui/css/reset.css', $plugdir), array(), '20120208', 'all');
    wp_register_style('swaggerui-screen', plugins_url('lib/swagger-ui/css/screen.css', $plugdir), array(), '20120208', 'all');
    wp_register_style('ecapi-custom', plugins_url('css/style.css', $plugdir), array(), '20120208', 'all');
    
    // wp_enqueue_style('swaggerui-reset');
    wp_enqueue_style('swaggerui-screen');
    wp_enqueue_style('ecapi-custom');
}

/*************************
 ********* INIT **********
 *************************/

add_action('wp_enqueue_scripts', 'wptuts_scripts_with_the_lot');
add_action('wp_enqueue_scripts', 'wptuts_styles_with_the_lot');
add_action('plugins_loaded', array(
    'DataApis',
    'get_instance'
));
add_action('admin_menu', 'ecapi_admin_add_page_settings');
add_action('admin_init', 'ecapi_admin_init');
