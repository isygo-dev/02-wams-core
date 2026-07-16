<!DOCTYPE html>
<html dir="ltr" xmlns="http://www.w3.org/1999/xhtml"
      lang="en">
<head>
    <meta charset="UTF-8">
    <meta content="width=device-width, initial-scale=1" name="viewport">
    <meta name="x-apple-disable-message-reformatting">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta content="telephone=no" name="format-detection">
    <title>Workflow board updated</title>
    <!--[if gte mso 9]>
    <xml>
        <o:OfficeDocumentSettings>
            <o:AllowPNG></o:AllowPNG>
            <o:PixelsPerInch>96</o:PixelsPerInch>
        </o:OfficeDocumentSettings>
    </xml>
    <![endif]-->
    <style type="text/css">
        body, table, td {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
        }

        body {
            width: 100% !important;
            height: 100% !important;
            padding: 0;
            margin: 0;
            background-color: #F4F5F7;
        }

        table {
            border-collapse: collapse;
        }

        a {
            color: #0C66E4;
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
<body style="width:100%;height:100%;padding:0;margin:0;background-color:#F4F5F7">
<div style="display:none;max-height:0;overflow:hidden;opacity:0">
    ${V_ITEM_TITLE} moved from ${V_FROM_STATE} to ${V_TO_STATE} on ${V_BOARD_NAME}. &#8203;&#8203;&#8203;&#8203;&#8203;&#8203;&#8203;&#8203;&#8203;&#8203;
</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#F4F5F7">
    <tr>
        <td align="center" style="padding:24px 12px">
            <table role="presentation" class="wrap" width="600" cellpadding="0" cellspacing="0"
                   style="width:600px;max-width:600px">

                <!-- Brand header -->
                <tr>
                    <td align="center" style="padding-bottom:16px">
                        <a href="${V_TENANT_URL!"#"}" target="_blank" style="text-decoration:none">
                            <img src="https://isygo-it.eu/assets/images/logo.png" width="140" alt="Logo"
                                 style="display:block;border:0;outline:none;text-decoration:none">
                        </a>
                    </td>
                </tr>

                <!-- Content card -->
                <tr>
                    <td class="pad"
                        style="background-color:#FFFFFF;border:1px solid #DFE1E6;border-radius:8px;padding:40px">

                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                            <tr>
                                <td align="center" style="padding-bottom:20px">
                                    <table role="presentation" cellpadding="0" cellspacing="0" width="64" height="64"
                                           style="width:64px;height:64px;background-color:#F3F0FD;border-radius:32px">
                                        <tr>
                                            <td align="center" valign="middle">
                                                <svg width="27" height="27" viewBox="0 0 24 24" fill="none"
                                                     xmlns="http://www.w3.org/2000/svg" role="img"
                                                     aria-label="Workflow board">
                                                    <rect x="3" y="4" width="18" height="16" rx="2" stroke="#6E5DC6"
                                                          stroke-width="1.6"/>
                                                    <path d="M8 8v9M14 8v5M18 8v11" stroke="#6E5DC6" stroke-width="1.6"
                                                          stroke-linecap="round"/>
                                                </svg>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td align="center" style="padding-bottom:12px">
                                    <h1 style="margin:0;font-size:22px;line-height:28px;font-weight:600;color:#172B4D">
                                        Workflow board updated</h1>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding-bottom:20px;font-size:15px;line-height:23px;color:#44546F">
                                    Hi ${V_FULLNAME},<br><br>
                                    The board <strong>${V_BOARD_NAME}</strong> was just updated. Here's what changed:
                                </td>
                            </tr>
                            <tr>
                                <td style="padding-bottom:24px">
                                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                                           style="background-color:#F4F5F7;border:1px solid #DFE1E6;border-radius:8px">
                                        <tr>
                                            <td style="padding:16px 20px">
                                                <div style="font-size:13px;color:#8590A2;padding-bottom:10px">${V_ITEM_TITLE}</div>
                                                <table role="presentation" cellpadding="0" cellspacing="0">
                                                    <tr>
                                                        <td style="background-color:#FFECEB;color:#AE2E24;font-size:13px;font-weight:600;padding:5px 12px;border-radius:20px">${V_FROM_STATE}</td>
                                                        <td style="padding:0 10px">
                                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                                                                 xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                                                                <path d="M4 12h15M13 6l6 6-6 6" stroke="#8590A2"
                                                                      stroke-width="1.8" stroke-linecap="round"
                                                                      stroke-linejoin="round"/>
                                                            </svg>
                                                        </td>
                                                        <td style="background-color:#DFFCEC;color:#216E4E;font-size:13px;font-weight:600;padding:5px 12px;border-radius:20px">${V_TO_STATE}</td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td align="center" style="padding-bottom:20px">
                                    <a href="${V_TENANT_URL!"#"}" target="_blank"
                                       style="display:inline-block;background-color:#0C66E4;color:#FFFFFF;font-size:15px;font-weight:600;text-decoration:none;padding:13px 28px;border-radius:6px">
                                        View workflow board
                                    </a>
                                </td>
                            </tr>
                            <tr>
                                <td style="font-size:13px;line-height:20px;color:#8590A2">
                                    Didn't expect this change? Please contact your tenant administrator.
                                </td>
                            </tr>
                        </table>

                    </td>
                </tr>

                <!-- Support strip -->
                <tr>
                    <td class="pad" style="padding:24px 40px 0">
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                            <tr>
                                <#if V_TENANT_PHONE?? && V_TENANT_PHONE != "Missed">
                                    <td align="left" style="font-size:13px;color:#44546F;padding:6px 0">
                                        <a href="tel:${V_TENANT_PHONE}" style="color:#44546F;text-decoration:none">
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                                                 xmlns="http://www.w3.org/2000/svg"
                                                 style="vertical-align:-2px;margin-right:6px" aria-hidden="true">
                                                <path d="M6.6 10.8a15.5 15.5 0 0 0 6.6 6.6l2.2-2.2a1 1 0 0 1 1-.25 11 11 0 0 0 3.4.55 1 1 0 0 1 1 1V20a1 1 0 0 1-1 1A17 17 0 0 1 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1 11 11 0 0 0 .55 3.4 1 1 0 0 1-.25 1L6.6 10.8Z"
                                                      stroke="#626F86" stroke-width="1.5" stroke-linejoin="round"/>
                                            </svg>${V_TENANT_PHONE}
                                        </a>
                                    </td>
                                </#if>
                                <#if V_TENANT_EMAIL?? && V_TENANT_EMAIL != "Missed">
                                    <td align="right" style="font-size:13px;color:#44546F;padding:6px 0">
                                        <a href="mailto:${V_TENANT_EMAIL}" style="color:#44546F;text-decoration:none">
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                                                 xmlns="http://www.w3.org/2000/svg"
                                                 style="vertical-align:-2px;margin-right:6px" aria-hidden="true">
                                                <rect x="3" y="5" width="18" height="14" rx="2" stroke="#626F86"
                                                      stroke-width="1.5"/>
                                                <path d="m3.5 6 8.5 6 8.5-6" stroke="#626F86" stroke-width="1.5"
                                                      stroke-linecap="round" stroke-linejoin="round"/>
                                            </svg>${V_TENANT_EMAIL}
                                        </a>
                                    </td>
                                </#if>
                            </tr>
                        </table>
                    </td>
                </tr>

                <!-- Footer -->
                <tr>
                    <td align="center" style="padding:24px 12px 12px;font-size:12px;line-height:18px;color:#8590A2">
                        <#if V_TENANT_ADDRESS?? && V_TENANT_ADDRESS != "Missed">${V_TENANT_ADDRESS}<br></#if>
                        This is an automated message, please don't reply directly to this email.
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
