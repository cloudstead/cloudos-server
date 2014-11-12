var rcs_skin_version = 3;
var rcs_skin = new function() {
    this.runBeforeReady = function() {
        if ($("#login-form #logo").length) {
            $("#login-form").append("<div id='company-name'>" + $("#login-form #logo").attr("alt") + "</div>");
            $("#login-form .box-inner").prepend("<h1>" + $("#rcmloginsubmit").attr("value") + "</h1>");
            return
        }
        if (rcs_common.pluginLoaded()) {
            if (rcmail.env.rcs_mobile) {
                $("#topnav a.button-logout").after("<a class='button-skin-options' href='javascript:void(0)' onclick='rcs_mobile.popup(\"skin-options\")'><span style='background-color: #b0263b'></span><span style='background-color: #00860e'></span><span style='background-color: #0075c8'></span><span style='background-color: #8d2297'></span></a>")
            } else {
                $("#topnav a.button-logout").before("<a class='button-skin-options' href='javascript:void(0)' onclick='rcs_skin.showOptions(this)'></a>")
            }
        }
        var d = "0075c8";
        var a = [
            ["df5aad", "ed6fbd", "c34292", "c34292"],
            ["b0263b", "be364b", "9e172b", "9e172b"],
            ["d74c1b", "e35b2b", "c33c0d", "c33c0d"],
            ["ff9022", "fe9834", "ea7a09", "c0650b"],
            ["83b600", "92c709", "75a201", "648b00"],
            ["00860e", "089817", "00760d", "00760d"],
            ["00b2b3", "03c3c4", "019fa0", "008081"],
            ["00829a", "0790a9", "00748a", "00748a"],
            ["03a2cc", "5bbcff", "45B4FF", "0076c6"],
            ["0075c8", "138be1", "005797", "005797"],
            ["3c2cb6", "4f40c4", "2c1da4", "2c1da4"],
            ["8d2297", "ac3cb6", "791482", "791482"],
            ["004e8d", "0c60a4", "00447a", "00447a"],
            ["001b41", "00265b", "093572", "093572"],
            ["5a0600", "78130c", "3f0400", "3f0400"],
            ["3a0300", "67100c", "67100c", "67100c"],
            ["585858", "767676", "3f3f3f", "3f3f3f"],
            ["000000", "3f3f3f", "3f3f3f", "3f3f3f"]
        ];
        var m = ".color-%1 .uibox .boxtitle,.color-%1 input[type=button],.color-%1 .uibox .listing thead td,.color-%1 .records-table thead td,.color-%1 #topnav,.color-%1 #topline,.color-%1 input.button.mainaction:active,.color-%1 ul#planner_controls li a,.rcs-mobile.color-%1 #main-menu,.rcs-mobile.color-%1 .popup-close,.rcs-mobile.color-%1 #mailboxlist li.mailbox a .unreadcount,.rcs-mobile.color-%1 #messagelistcontainer .boxpagenav a.icon,.rcs-mobile.color-%1 #messagelistcontainer .pagenav a.button,.rcs-mobile.color-%1 #messagestack div,.rcs-mobile.color-%1 #message-objects div a.button,.rcs-mobile.color-%1 .boxfooter .listbutton{ background-color: #%1; }.color-%1 .popupmenu,.color-%1 #messagestack div{ border-color: #%1; }.rcs-mobile.color-%1 #settings-right div.uibox{ border-color: #%1 !important; }.color-%1 input[type=button]:hover,.color-%1 a.rowLink:hover,.color-%1 #remote-objects-message a:hover{ background-color: #%2 !important; }.color-%1 #topnav a.button-selected,.color-%1 #topnav a:hover,.color-%1 body.mailbox #inboxButton a{ background-color: #%3; }.color-%1 .toolbar a.button:before,.rcs-mobile.color-%1 #mailboxlist li.mailbox div.treetoggle,.rcs-mobile.color-%1 #directorylist li.addressbook div.collapsed,.rcs-mobile.color-%1 #directorylist li.addressbook div.expanded{ color: #%1; }.color-%1 .toolbar a.button,.color-%1 a,.color-%1 #main-menu a.active{ color: #%4; }";
        var k = "";
        for (var h = 0; h < a.length; h++) {
            var l = m;
            for (var g = 0; g < 4; g++) {
                l = l.replace(new RegExp("%" + (g + 1), "g"), a[h][g])
            }
            k += l
        }
        k = "<style>" + k + "</style>";
        var e = "";
        for (var h = 0; h < a.length; h++) {
            e += "<a class='color-box' onclick='rcs_skin.changeColor(&quot;" + a[h][0] + "&quot;)' style='background:#" + a[h][0] + " !important'></a>";
            if ((h + 1) % 6 == 0) {
                e += "<br />"
            }
        }
        var b = "<div id='skin-options' class='popupmenu'>" + (this.mobile() ? "<h5>" + rcs_mobile.label_options + "</h5>" : "") + "<div id='skin-color-select'>" + e + "</div><div class='loader'></div></div>";
        $("#messagestack").after(k + b);
        var c = typeof rcmail.env.rcs_color !== "undefined" ? rcmail.env.rcs_color : false;
        var f = false;
        for (var h = 0; h < a.length; h++) {
            if (f == a[h][0]) {
                f = c;
                break
            }
        }
        this.changeColor(f ? f : d, false)
    };
    this.runOnReady = function() {
        if (this.mobile()) {
            $("#skin-options a").on("click", function() {
                rcs_mobile.popupHideAll()
            });
            $("#attachment-list .delete").html("");
            $(".settings-button").each(function() {})
        }
        $(".button-skin-options .tooltip").html(rcs_skin.label_skin);
        $("#planner_controls a").not("#today").html("")
    };
    this.changeColor = function(a, c) {
        c = typeof c !== "undefined" ? c : true;
        var b = $("body");
        b.removeClass(function(e, d) {
            return (d.match(/color-\S+/g) || []).join(" ")
        });
        b.addClass("color-" + a);
        rcmail.save_pref({
            name: "rcs_skin_color_" + rcmail.env.rcs_skin,
            value: a
        })
    };
    this.showOptions = function(a) {
        if (!$("#skin-options").length) {
            return
        }
        a = $(a);
        $("#skin-options").css("top", a.offset().top + a.outerHeight());
        UI.show_popup("skin-options")
    };
    this.mobile = function() {
        return rcs_common.pluginLoaded() && rcmail.env.rcs_mobile
    }
};