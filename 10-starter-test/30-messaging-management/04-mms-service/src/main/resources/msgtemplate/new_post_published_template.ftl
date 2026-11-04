<!DOCTYPE html>
<html dir="ltr" xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="x-apple-disable-message-reformatting">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="format-detection" content="telephone=no">
    <title>Class timetable published</title>
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
                padding-left: 20px !important;
                padding-right: 20px !important;
            }
        }
    </style>
</head>
<body style="width:100%;height:100%;margin:0;padding:0;background-color:#F4F5F7">
<div style="display:none;max-height:0;overflow:hidden;opacity:0">
    The timetable for ${V_CLASS_NAME!"your class"} is published. A PDF copy is attached.
</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#F4F5F7">
    <tr>
        <td align="center" style="padding:24px 12px">
            <table role="presentation" class="wrap" width="600" cellpadding="0" cellspacing="0"
                   style="width:600px;max-width:600px">
                <tr>
                    <td align="center" style="padding-bottom:16px">
                        <a href="${V_TENANT_URL!"#"}" target="_blank" style="text-decoration:none">
                            <img src="https://isygo-it.eu/assets/images/logo.png" width="140" alt="Logo"
                                 style="display:block;border:0;outline:none;text-decoration:none">
                        </a>
                    </td>
                </tr>
                <tr>
                    <td class="pad"
                        style="background-color:#FFFFFF;border:1px solid #DFE1E3;border-radius:12px;padding:40px">
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                            <tr>
                                <td align="center" style="padding-bottom:20px">
                                    <table role="presentation" cellpadding="0" cellspacing="0" width="64" height="64"
                                           style="width:64px;height:64px;background-color:#E9F2FF;border-radius:32px">
                                        <tr>
                                            <td align="center" valign="middle">
                                                <span style="font-size:30px;color:#0C66E4">&#128197;</span>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td align="center" style="padding-bottom:12px">
                                    <h1 style="margin:0;font-size:22px;line-height:28px;font-weight:600;color:#172B4D">
                                        Class timetable published</h1>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding-bottom:20px;font-size:15px;line-height:23px;color:#44546F">
                                    Hello ${V_FULLNAME!"there"},<br><br>
                                    The timetable for <strong>${V_CLASS_NAME!"your class"}</strong> is now published.
                                    Please find the complete schedule attached as a PDF.
                                </td>
                            </tr>
                            <tr>
                                <td style="padding-bottom:20px">
                                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                                           style="background-color:#F4F5F7;border:1px solid #DFE1E3;border-radius:8px">
                                        <tr>
                                            <td style="padding:16px 20px">
                                                <div style="font-size:16px;line-height:22px;font-weight:600;color:#172B4D">
                                                    ${V_TIMETABLE_NAME!"Timetable"}</div>
                                                <div style="padding-top:6px;font-size:14px;line-height:21px;color:#44546F">
                                                    ${V_START_DATE!"Not specified"} - ${V_END_DATE!"Not specified"}
                                                </div>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td style="font-size:13px;line-height:20px;color:#8590A2">
                                    Keep the attached schedule for reference. If you have questions, please contact
                                    your school.
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr>
                    <td align="center" style="padding:20px 12px;font-size:12px;line-height:18px;color:#8590A2">
                        This is an automated message from your school.
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
