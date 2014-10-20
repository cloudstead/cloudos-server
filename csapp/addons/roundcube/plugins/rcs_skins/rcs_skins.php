<?php
/**
 * Roundcubeskins.net plugin. See README for details.
 * Copyright 2014, Tecorama.
 */

class rcs_skins extends rcube_plugin
{
    public $allowed_prefs = array(
        "rcs_skin_color_alpha",
        "rcs_skin_color_bravo",
        "rcs_skin_color_corporate",
        "rcs_skin_color_iclassic",
        "rcs_skin_color_outlook",
        "rcs_skin_color_litecube",
        "rcs_skin_color_w21",
    );

    private $phone = false;
    private $tablet = false;
    private $desktop = false;
    private $mobile = false;
    private $device = false;
    private $skin = false;

    private $skins = array(
        "alpha",
        "bravo",
        "corporate",
        "iClassic",
        "litecube",
        "litecube-f",
        "outlook",
        "w21",
    );

    private $plugins = array(
        "calendar",
        "calendar_plus",
        "compose_in_taskbar",
        "jappix4roundcube",
        "keyboard_shortcuts",
        "message_highlight",
        "moreuserinfo",
        "nabble",
        "persistent_login",
        "planner",
        "plugin_manager",
        "settings",
        "timepicker",
        "summary",
        "vcard_send",
    );

    /**
     * Initializes the plugin.
     */
    public function init()
    {
        $this->setDevice();

        $this->add_hook("config_get", array($this, "getConfig"));
        $this->add_hook('startup', array($this, 'startup'));
        $this->add_hook('render_page', array($this, 'renderPage'));
    }

    /**
     * Hook retrieving config options (including user settings)
     */
    function getConfig($args)
    {
        // disable preview pane on mobile devices

        if ($this->mobile && $args['name'] == "preview_pane") {
           $args['result'] = false;
           return $args;
        }

        // Substitute the skin name retrieved from the config file with "larry" for the plugins that treat larry-based
        // skins as "classic."

        if ($args['name'] != "skin" || !in_array($args['result'], $this->skins)) {
            return $args;
        }

        // check php version to use the right parameters
        if (version_compare(phpversion(), "5.3.6", "<")) {
            $options = false;
        } else {
            $options = DEBUG_BACKTRACE_IGNORE_ARGS;
        }

        // when passing 4 as the second parameter in php < 5.4, debug_backtrace will return null
        if (version_compare(phpversion(), "5.4.0", "<")) {
            $trace = debug_backtrace($options);
        } else {
            $trace = debug_backtrace($options, 4);
        }

        if (!empty($trace[3]['file']) && in_array(basename(dirname($trace[3]['file'])), $this->plugins)) {
            $args['result'] = "larry";
        }

        return $args;
    }

    /**
     * Executes at the start of the program run.
     */
    public function startup()
    {
        $rcmail = rcmail::get_instance();
        $prefs = $rcmail->user->get_prefs();

        $this->skin = isset($prefs['skin']) ? $prefs['skin'] : $rcmail->config->get('skin');
        $this->color = isset($prefs["rcs_skin_color_{$this->skin}"]) ? $prefs["rcs_skin_color_{$this->skin}"] : false;

        // set javascript environment variables

        $rcmail->output->set_env('rcs_phone', $this->phone);
        $rcmail->output->set_env('rcs_tablet', $this->tablet);
        $rcmail->output->set_env('rcs_mobile', $this->mobile);
        $rcmail->output->set_env('rcs_desktop', $this->desktop);
        $rcmail->output->set_env('rcs_device', $this->device);
        $rcmail->output->set_env('rcs_color', $this->color);
        $rcmail->output->set_env('rcs_skin', $this->skin);

        // disable composing in html on mobile devices

        if ($this->mobile) {
            global $CONFIG;
            $CONFIG['htmleditor'] = false;
        }
    }

    /**
     * Makes modifications to the html output contents.
     */
    public function renderPage($p)
    {
        if ($this->phone) {
            $class = "rcs-mobile rcs-phone";
        } else if ($this->tablet) {
            $class = "rcs-mobile rcs-tablet";
        } else {
            $class = "rcs-desktop";
        }

        $p['content'] = str_replace(
            "</body>",
            "<script>".
            "$('body').addClass('$class');".
            "</script>".
            "</body>",
            $p['content']
        );

        return $p;
    }

    /**
     * Sets the device based on detected user agent or url parameters.
     * You can use ?phone=1, ?phone=0, ?tablet=1 or ?tablet=0 to force the phone or tablet mode.
     */
    private function setDevice()
    {
        require_once("Mobile_Detect.php");
        $detect = new Mobile_Detect();

        $this->mobile = $detect->isMobile();
        $tablet = $detect->isTablet();

        if (isset($_GET['phone'])) {
            $this->phone = (bool)$_GET['phone'];
        } else {
            $this->phone = $this->mobile && !$tablet;
        }

        if (isset($_GET['tablet'])) {
            $this->tablet = (bool)$_GET['tablet'];
        } else {
            $this->tablet = $tablet;
        }

        $this->desktop = !$this->mobile;

        if ($this->phone) {
            $this->device = "phone";
        } else if ($this->tablet) {
            $this->device = "tablet";
        } else {
            $this->device = "desktop";
        }
    }


}

