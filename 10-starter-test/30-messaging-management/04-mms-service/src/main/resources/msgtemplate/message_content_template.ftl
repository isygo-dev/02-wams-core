<!DOCTYPE html>
<html dir="ltr" xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="x-apple-disable-message-reformatting">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="format-detection" content="telephone=no">
    <title>${(V_MESSAGE_TITLE!"Message")?html}</title>
    <style type="text/css">
        body, table, td {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
        }

        body {
            width: 100% !important;
            height: 100% !important;
            margin: 0;
            padding: 0;
            background-color: #F4F5F7;
        }

        table {
            border-collapse: collapse;
        }

        @media only screen and (max-width: 600px) {
            .wrap {
                width: 100% !important;
            }

            .pad {
                padding: 24px !important;
            }
        }
    </style>
</head>
<body style="width:100%;height:100%;margin:0;padding:0;background-color:#F4F5F7">
<div style="display:none;max-height:0;overflow:hidden;opacity:0">
    ${(V_MESSAGE_PREHEADER!"")?html}
</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#F4F5F7">
    <tr>
        <td align="center" style="padding:24px 12px">
            <table role="presentation" class="wrap" width="600" cellpadding="0" cellspacing="0"
                   style="width:600px;max-width:600px">
                <tr>
                    <td align="center" style="padding-bottom:16px">
                        <a href="${(V_TENANT_URL!"#")?html}" target="_blank" style="text-decoration:none">
                            <img src="https://isygo-it.eu/assets/images/logo.png" width="140" alt="Logo"
                                 style="display:block;border:0;outline:none;text-decoration:none">
                        </a>
                    </td>
                </tr>
                <tr>
                    <td class="pad"
                        style="background-color:#FFFFFF;border:1px solid #DFE1E3;border-radius:12px;padding:40px">
                        <h1 style="margin:0 0 20px;font-size:22px;line-height:28px;font-weight:600;color:#172B4D">
                            ${(V_MESSAGE_TITLE!"Message")?html}
                        </h1>
                        <div style="font-size:15px;line-height:23px;color:#44546F">
                            <!-- V_MESSAGE_CONTENT must contain trusted or sanitized HTML. -->
                            ${V_MESSAGE_CONTENT!""}
                        </div>
                    </td>
                </tr>
                <tr>
                    <td align="center" style="padding:20px 12px;font-size:12px;line-height:18px;color:#626F86">
                        ${(V_TENANT_NAME!"")?html}
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
