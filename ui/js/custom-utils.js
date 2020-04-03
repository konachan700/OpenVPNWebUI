function messageWarn(message) {
    $("#warning1").show();
    $("#warning1").html("<div class=\"alert alert-danger\" role=\"alert\">" + message + "</div>");
    setTimeout(function() {
        $("#warning1").hide();
    }, 2500);
}

function messageInfo(message) {
    $("#warning1").show();
    $("#warning1").html("<div class=\"alert alert-success\" role=\"alert\">" + message + "</div>");
    setTimeout(function() {
        $("#warning1").hide();
    }, 2500);
}

function showTableTemplatesEx(templateUrl, headerName, arrayOfSettings) {
    $.ajax({
        url: templateUrl,
        dataType: "text",
        data: { },
        type: "GET",
        success: function (response) {
            $("#mainpage1").html(response);
            $("#global_header").text(headerName);
            for (var i=0; i<arrayOfSettings.length; i++) {
                $(arrayOfSettings[i].tableId).DataTable( {
                    "processing": true,
                    "ajax": arrayOfSettings[i].url,
                    "columns": arrayOfSettings[i].coloumnSettings,
                    "coloumnDefs": arrayOfSettings[i].coloumnDef
                } );
            }
        },
        error: function (errorThrown) {
            $("#mainpage1").text("Error: " + errorThrown.error);
        }
    });
}

function showTableTemplateEx(templateUrl, headerName, dataUrl, coloumnSettings, coloumnDef) {
    let ul1 = $("#additional-links");
    ul1.empty();

    $.ajax({
        url: templateUrl,
        dataType: "text",
        data: { },
        type: "GET",
        success: function (response) {
            $("#mainpage1").html(response);
            $("#global_header").text(headerName);
            $('#general-table').DataTable( {
                "processing": true,
                "ajax": dataUrl,
                "columns": coloumnSettings,
                "coloumnDefs": coloumnDef
            } );
        },
        error: function (errorThrown) {
            $("#mainpage1").text("Error: " + errorThrown.error);
        }
    });
}

function showTableTemplate(templateUrl, headerName, dataUrl, coloumnSettings) {
    showTableTemplateEx(templateUrl, headerName, dataUrl, coloumnSettings, [])
}

function showTemplate(templateUrl, headerName, dataUrl, callback) {
    let ul1 = $("#additional-links");
    ul1.empty();

    $.ajax({
        url: templateUrl,
        dataType: "text",
        data: { },
        type: "GET",
        success: function (response) {
            $("#mainpage1").html(response);
            $("#global_header").text(headerName);
            $.ajax({
                url: dataUrl,
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Accept", "application/json");
                },
                dataType: "json",
                data: { },
                type: "GET",
                success: function (response2) {
                    callback(response2);
                },
                error: function (errorThrown) {
                    $("#mainpage1").text("Error: " + errorThrown.error);
                }
            });
        },
        error: function (errorThrown) {
            $("#mainpage1").text("Error: " + errorThrown.error);
        }
    });
}

function checkUrl(url, callbackIfTrue, callbackIfFalse) {
    $.ajax({
        url: url,
        dataType: "json",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Accept", "application/json");
        },
        data: { },
        type: "GET",
        success: function (response) {
            if (response.data === "YES") {
                callbackIfTrue();
            } else {
                callbackIfFalse();
            }
        },
        error: function (errorThrown) {
            $("#mainpage1").text("Error: " + errorThrown.error);
        }
    });
}

function save_settings() {
    save_settings_ex(function (response) {});
}

function save_settings_ex(callback) {
    var obj = {};
    let elements = $("#cont_inputs_sett_1").find("input");
    for (var i=0; i<elements.length; i++) {
        let element = elements[i];
        if (element.id.startsWith("s_")) {
            obj[element.id] = element.value;
        }
    }
    $.ajax({
        url: "/openvpn/savecertsettings",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            xhr.setRequestHeader("Accept", "application/json");
        },
        dataType: "json",
        data: obj,
        type: "POST",
        success: function (response) {
            callback(response);
        },
        error: function (errorThrown) {
            alert(errorThrown.error);
        }
    });
}

function create_cert_ex() {
    $.ajax({
        url: "/openvpn/createcert",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            xhr.setRequestHeader("Accept", "application/json");
        },
        dataType: "json",
        data: {
            "name": $("#certNameField").val().replace(/[^A-Z0-9]+/ig, "_")
        },
        type: "POST",
        success: function (response) {
            index__show_openvpn_certs();
        },
        error: function (errorThrown) {
            alert(errorThrown.error);
        }
    });
}

function openvpn_certs__check_dh() {
    checkUrl("/openvpn/isdhexist",
        function() {
            $("#warning1").hide();
            let ul1 = $("#additional-links");

            ul1.append("<li class=\"nav-item active\"><a class=\"nav-link\" href=\"#\" id='addcertbtn'>Add certificate</a></li>");
            ul1.append("<li class=\"nav-item active\"><a class=\"nav-link\" href=\"#\" id='certsettingsbtn'>Settings</a></li>");

            $("#addcertbtn").click(function() {
                showTemplate("/tpl/openvpn-cert-add.html", "Add new certificate", "/openvpn/getsettings", function(resp) {
                    ul1.empty();
                    ul1.html("<button type=\"button\" class=\"btn btn-secondary btn-sm\" id=\"btn_cert_add_cancel_close\">" +
                        "&nbsp;&nbsp;&nbsp;Cancel&nbsp;&nbsp;&nbsp;</button>&nbsp;&nbsp;&nbsp;" +
                        "<button type=\"button\" class=\"btn btn-warning btn-sm\" id=\"btn_cert_add_ok_save\">&nbsp;&nbsp;&nbsp;Save&nbsp;&nbsp;&nbsp;</button>");
                    let elements = $("#cont_inputs_sett_1").find("input");
                    for (var i=0; i<elements.length; i++) {
                        let element = elements[i];
                        if (element.id.startsWith("s_")) {
                            element.value = resp.data[element.id];
                        }
                    }
                    $("#btn_cert_add_cancel_close").click(function() {
                        index__show_openvpn_certs();
                    });
                    $("#btn_cert_add_ok_save").click(function() {
                        if ($("#certNameField").val().trim().length < 1) {
                            messageWarn("Filename can't be empty");
                            return;
                        }

                        save_settings_ex(function (response) {
                            if (response.data === "OK") {
                                create_cert_ex();
                            } else {
                                messageWarn("Error. Can't create a certificate. See log for details.");
                            }
                        });
                    });
                });
            });

            $("#certsettingsbtn").click(function() {
                showTemplate("/tpl/openvpn-cert-settings.html", "Certificate settings", "/openvpn/getsettings", function(resp) {
                    ul1.empty();
                    ul1.html("<button type=\"button\" class=\"btn btn-secondary btn-sm\" id=\"btn_cert_cancel_close\">" +
                        "&nbsp;&nbsp;&nbsp;Cancel&nbsp;&nbsp;&nbsp;</button>&nbsp;&nbsp;&nbsp;" +
                        "<button type=\"button\" class=\"btn btn-warning btn-sm\" id=\"btn_cert_ok_save\">&nbsp;&nbsp;&nbsp;Save&nbsp;&nbsp;&nbsp;</button>");

                    let elements = $("#cont_inputs_sett_1").find("input");
                    for (var i=0; i<elements.length; i++) {
                        let element = elements[i];
                        if (element.id.startsWith("s_")) {
                            element.value = resp.data[element.id];
                        }
                    }

                    $("#btn_cert_cancel_close").click(function() {
                        index__show_openvpn_certs();
                    });
                    $("#btn_cert_ok_save").click(function() {
                        save_settings();
                    });
                });
            });
        },
        function() {
            messageWarn("We are generating the Diffie-Hellman (DH) parameters. It's very long process. Please, wait...");
            setTimeout(openvpn_certs__check_dh, 2500);
        });
}

function index__show_openvpn_certs() {
    showTableTemplate("/tpl/openvpn-certs.html", "OpenVPN certificates", "/openvpn/allcert", [
        { "data": "fileName" },
        { "data": "validFrom" },
        { "data": "validTo" },
        { "data": "subject" },
        { "data": "fileName",
            render: function(data, type, full) {
                return "<a href='/openvpn/certdl?type=ovpn&file="+encodeURI(data)+"'><u>ovpn</u></a>&nbsp;&nbsp;" +
                    "<a href='/openvpn/certdl?type=zip&file="+encodeURI(data)+"'><u>zip</u></a>";
            }
        }
    ]);
}

function index__show_opened_ports() {
    showTableTemplatesEx(
        "/tpl/opened-ports.html",
        "Opened ports",
        [
            {
                "tableId": "#general-table-tcp",
                "url": "/netstat/tupln?name=tcp",
                "coloumnSettings": [
                    { "data": "port" },
                    { "data": "ip" },
                    { "data": "protocol" }
                ],
                "coloumnDefs": []
            },{
                "tableId": "#general-table-udp",
                "url": "/netstat/tupln?name=udp",
                "coloumnSettings": [
                    { "data": "port" },
                    { "data": "ip" },
                    { "data": "protocol" }
                ],
                "coloumnDefs": []
            }
        ]
    );
}

function index__show_configs_win() {
    showTemplate("/tpl/openvpn-configs.html", "OpenVPN configurations", "/openvpn/readconfig", function(resp) {
        let ul1 = $("#additional-links");
        ul1.empty();
        ul1.html("<button type=\"button\" class=\"btn btn-secondary btn-sm\" id=\"btn_cancel_close\">" +
            "&nbsp;&nbsp;&nbsp;Reset&nbsp;&nbsp;&nbsp;</button>&nbsp;&nbsp;&nbsp;" +
            "<button type=\"button\" class=\"btn btn-warning btn-sm\" id=\"btn_ok_save\">&nbsp;&nbsp;&nbsp;Save&nbsp;&nbsp;&nbsp;</button>");
        $("#ta_server_config").val(atob(resp.data.server));
        $("#ta_client_config").val(atob(resp.data.client));
        $("#btn_ok_save").click(function() {
            $.ajax({
                url: "/openvpn/saveconfig",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                    xhr.setRequestHeader("Accept", "application/json");
                },
                dataType: "json",
                data: {
                    "server": btoa($("#ta_server_config").val()),
                    "client": btoa($("#ta_client_config").val())
                },
                type: "POST",
                success: function (response) {
                    messageWarn("Configuration saved at "+ new Date());
                },
                error: function (errorThrown) {
                    alert(errorThrown.error);
                }
            });
        });
        $("#btn_cancel_close").click(function() {
            index__show_configs_win();
        });
    });
}

function index__show_openvpnsvc_engine(state) {
    $.ajax({
        url: "/openvpn/" + state,
        dataType: "json",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Accept", "application/json");
        },
        data: { },
        type: "GET",
        success: function (response) {
            if (state === "run") {
                if (response.data === "OK") {
                    messageInfo("Server started at " + new Date());
                } else if (response.data === "AR") {
                    messageInfo("Server already started. Do nothing.");
                } else {
                    messageWarn("Error while server starting. See logs for details.");
                }
            } else {
                if (response.data === "OK") {
                    messageInfo("Server stopped at " + new Date());
                } else {
                    messageWarn("Error while server stopping. See logs for details.");
                }
            }
        },
        error: function (errorThrown) {
            $("#mainpage1").text("Error: " + errorThrown.error);
        }
    });
}

function index__show_openvpnsvc() {
    showTemplate("/tpl/openvpn-svc.html", "OpenVPN service", "/openvpn/readconfig", function(resp) {
        let ul1 = $("#additional-links");
        ul1.empty();
        ul1.html("<button type=\"button\" class=\"btn btn-danger btn-sm collapse\" id=\"btn_stop\">" +
            "&nbsp;&nbsp;&nbsp;Stop&nbsp;&nbsp;&nbsp;</button>&nbsp;&nbsp;&nbsp;" +
            "<button type=\"button\" class=\"btn btn-success btn-sm collapse\" id=\"btn_start\">" +
            "&nbsp;&nbsp;&nbsp;Start&nbsp;&nbsp;&nbsp;</button>");

        stompClient.send("/app/general", {}, "{\"cmd\":\"IS_OVPN_RUN\", \"data\":{}}");
        stompClient.send("/app/general", {}, "{\"cmd\":\"LOGS\", \"data\":{}}");

        $("#btn_start").click(function() {
            $("#btn_start").hide();
            $("#btn_stop").hide();
            index__show_openvpnsvc_engine("run");
        });
        $("#btn_stop").click(function() {
            $("#btn_start").hide();
            $("#btn_stop").hide();
            index__show_openvpnsvc_engine("stop");
        });
    });
}