<?php
///////////////////////////////////////////////////////////////////////////////
// OBM - File : payment_index.php 
//     - Desc : payment Index File
// 2001-08-21 Aliacom
///////////////////////////////////////////////////////////////////////////////
// $Id$ //
///////////////////////////////////////////////////////////////////////////////

$path = "..";
$module = "payment";
$obminclude = getenv("OBM_INCLUDE_VAR");
if ($obminclude == "") $obminclude = "obminclude";
require("$obminclude/global.inc");
$params = get_payment_params();
page_open(array("sess" => "OBM_Session", "auth" => $auth_class_name, "perm" => "OBM_Perm"));
require("$obminclude/global_pref.inc");
require("payment_display.inc");
require("payment_query.inc");
require("payment_js.inc");
require_once("$obminclude/of/of_category.inc");

get_payment_action();
$perm->check_permissions($module, $action);

update_last_visit("payment", $params["payment_id"], $action);
page_close();


///////////////////////////////////////////////////////////////////////////////
// Main Program                                                              //
///////////////////////////////////////////////////////////////////////////////
if ($action == "index") {
///////////////////////////////////////////////////////////////////////////////
  $display["search"] = dis_payment_search_form($params);
  if ($_SESSION['set_display'] == "yes") {
    $display["result"] = dis_payment_search_list($params);
  } else {
    $display["msg"] .= display_info_msg($l_no_display);
  }

} elseif ($action == "search") {
///////////////////////////////////////////////////////////////////////////////
  $display["search"] = dis_payment_search_form($params);
  $display["result"] = dis_payment_search_list($params);
  
} elseif ($action == "new") {
///////////////////////////////////////////////////////////////////////////////
  require("payment_js.inc");
  $display["detail"] = dis_payment_form($action, $params);

} elseif ($action == "insert") {
///////////////////////////////////////////////////////////////////////////////
  if (check_payment_data_form("", $params)) {
    $id = run_query_payment_insert($params);
    if ($id > 0) {
      $params["payment_id"] = $id;
      $display["msg"] = display_ok_msg ("$l_payment : $l_insert_ok");
      $display["detail"] = dis_payment_consult($params);
    } else {
      $display["msg"] = display_err_msg ("$l_payment : $l_insert_error");
      $display["detail"] = dis_payment_form($action, $params);
    }
  // Form data are not valid
  } else {
    $display["msg"] .= display_warn_msg($l_invalid_data . " : " . $err["msg"]);
    require("payment_js.inc");
    $display["detail"] = dis_payment_form($action, $params);
  }

} elseif ($action == "detailconsult") {
///////////////////////////////////////////////////////////////////////////////
  $display["detail"] = dis_payment_consult($params);

} elseif ($action == "detailupdate") {
///////////////////////////////////////////////////////////////////////////////
  require("payment_js.inc");
  $display["detail"] = dis_payment_form($action, $params);

} elseif ($action == "detail_invoice") {
///////////////////////////////////////////////////////////////////////////////
  require("payment_js.inc");
  $display["detail"] = dis_payment_invoice($params);

} else if ($action == "detailduplicate") {
///////////////////////////////////////////////////////////////////////////////
  $params["id_duplicated"] = $params["payment_id"];
  $params["payment_id"] = "";
  $display["detail"] = dis_payment_form($action, $params);

} elseif ($action == "update") {
///////////////////////////////////////////////////////////////////////////////
  if (check_payment_data_form($params["payment_id"], $params)) {
    $retour = run_query_payment_update($params["payment_id"], $params);
    if ($retour) {
      $display["msg"] .= display_ok_msg("$l_payment : $l_update_ok");
    } else {
      $display["msg"] .= display_err_msg("$l_payment : $l_update_error");
    }
    $display["detail"] = dis_payment_consult($params);
  } else {
    $display["msg"] .= display_warn_msg($l_invalid_data . " : " . $err["msg"]);
    $display["detail"] = dis_payment_form($action, $params);
  }

} elseif ($action == "invoice_update") {
///////////////////////////////////////////////////////////////////////////////
  if (check_payment_invoice_data_form($params)) {
    $retour = run_query_payment_invoice_update($params["payment_id"], $params);
    if ($retour) {
      $display["msg"] .= display_ok_msg("$l_payment : $l_update_ok");
    } else {
      $display["msg"] .= display_err_msg("$l_payment : $l_update_error");
    }
    $display["detail"] = dis_payment_consult($params);
  } else {
    $display["msg"] .= display_warn_msg($l_invalid_data . " : " . $err["msg"]);
    $display["detail"] = dis_payment_invoice($params);
  }

} elseif ($action == "check_delete") {
///////////////////////////////////////////////////////////////////////////////
  if (check_can_delete_payment($params["payment_id"])) {
    require("payment_js.inc");
    $display["msg"] .= display_info_msg($ok_msg, false);
    $display["detail"] = dis_can_delete_payment($params["payment_id"]);
  } else {
    $display["msg"] .= display_warn_msg($err["msg"], false);
    $display["msg"] .= display_warn_msg($l_cant_delete, false);
    $display["detail"] = dis_payment_consult($params);
  }

} elseif ($action == "delete") {
///////////////////////////////////////////////////////////////////////////////
  if (check_can_delete_payment($params["payment_id"])) {
    $retour = run_query_payment_delete($params["payment_id"]);
    if ($retour) {
      $display["msg"] .= display_ok_msg("$l_payment : $l_delete_ok");
    } else {
      $display["msg"] .= display_err_msg("$l_payment : $l_delete_error");
    }
    $display["search"] = dis_payment_search_form($params);
  } else {
    $display["msg"] .= display_warn_msg($err["msg"], false);
    $display["msg"] .= display_warn_msg($l_cant_delete, false);
    $display["detail"] = dis_payment_consult($params);
  }
  
} elseif ($action == "invoice_add") {
///////////////////////////////////////////////////////////////////////////////
  if (($params["invoice_id"] > 0) && ($params["payment_id"] > 0)) {
    run_query_payment_invoice_insert($params);
    $display["msg"] .= display_ok_msg("$l_invoice_added");
  } else {
    $display["msg"] .= display_err_msg("$l_no_invoice_added");
  }
  $display["detail"] = dis_payment_invoice($params);

} elseif ($action == "display") {
///////////////////////////////////////////////////////////////////////////////
  $prefs = get_display_pref($obm["uid"],"payment",1);
  $display["detail"] = dis_payment_display_pref ($prefs);

}elseif ($action == "dispref_display") {
///////////////////////////////////////////////////////////////////////////////
  update_display_pref($params);
  $prefs = get_display_pref($obm["uid"], "payment", 1);
  $display["detail"] = dis_payment_display_pref($prefs);

}elseif ($action == "dispref_level") {
///////////////////////////////////////////////////////////////////////////////
  update_display_pref($params);
  $prefs = get_display_pref($obm["uid"], "payment", 1);
  $display["detail"] = dis_payment_display_pref($prefs);
}


///////////////////////////////////////////////////////////////////////////////
// Display
///////////////////////////////////////////////////////////////////////////////
$display["head"] = display_head("$l_payment");
update_payment_action();
$display["header"] = display_menu($module);
$display["end"] = display_end();
display_page($display);


///////////////////////////////////////////////////////////////////////////////
// Stores Payment parameters transmitted in $payment hash
// returns : $payment hash with parameters set
///////////////////////////////////////////////////////////////////////////////
function get_payment_params() {

  // Get global params
  $params = get_global_params("Payment");

  if (isset($params)) {
    $nb_inv = 0;
    while ( list( $key, $value ) = each($params) ) {
      if (strcmp(substr($key, 0, 9),"data-inv-") == 0) {
        $nb_inv++;
        $inv_num = substr($key, 9);
        $params["invoices"][$inv_num] = $value;
      }
    }
    $params["invoices_nb"] = $nb_inv;
  }

  return $params;
}


//////////////////////////////////////////////////////////////////////////////
// Payment actions
//////////////////////////////////////////////////////////////////////////////
function get_payment_action() {
  global $params, $actions, $path;
  global $l_header_find,$l_header_new,$l_header_update,$l_header_delete;
  global $l_header_consult, $l_header_display, $l_header_admin;
  global $l_header_duplicate, $l_module_invoice, $l_header_link_invoice;
  global $cright_read, $cright_write, $cright_read_admin, $cright_write_admin;

  $id = $params["payment_id"];

//Index
  $actions["payment"]["index"] = array (
    'Name'     => $l_header_find,
    'Url'      => "$path/payment/payment_index.php?action=index",
    'Right'    => $cright_read,
    'Condition'=> array ('all') 
                                        );

// Search
  $actions["payment"]["search"] = array (
    'Url'      => "$path/payment/payment_index.php?action=search",
    'Right'    => $cright_read,
    'Condition'=> array ('None') 
                                        );

// New
  $actions["payment"]["new"] = array (
    'Name'     => $l_header_new,
    'Url'      => "$path/payment/payment_index.php?action=new",
    'Right'    => $cright_write,
    'Condition'=> array ('all') 
                                     );

// Detail Consult
  $actions["payment"]["detailconsult"] = array (
    'Name'     => $l_header_consult,
    'Url'      => "$path/payment/payment_index.php?action=detailconsult&amp;payment_id=".$params["payment_id"],
    'Right'    => $cright_read,
    'Condition'=> array ('detail_invoice', 'detailupdate', 'invoice_add', 'invoice_update', 'update')
                                        );

// Detail Consult Invoice
  $actions["payment"]["detail_invoice"] = array (
    'Name'     => $l_module_invoice,
    'Url'      => "$path/payment/payment_index.php?action=detail_invoice&amp;payment_id=".$params["payment_id"],
    'Right'    => $cright_read,
    'Condition'=> array ('detailconsult', 'detailupdate', 'invoice_add', 'invoice_update', 'update') 
                                        );

// Sel invoice : Invoice selection (menu)
  $actions["payment"]["sel_invoice"] = array (
    'Name'     => $l_header_link_invoice,
    'Url'      => "$path/invoice/invoice_index.php?action=ext_get_id&amp;popup=1&amp;ext_action=invoice_add&amp;ext_url=".urlencode($path."/payment/payment_index.php?action=invoice_add&amp;payment_id=$id&amp;sel_invoice_id=")."&amp;ext_id=".$params["payment_id"]."&amp;ext_target=$l_payment",
    'Right'    => $cright_write,
    'Popup'    => 1,
    'Target'   => $l_payment,
    'Condition'=> array ('detailconsult','detail_invoice','update','invoice_add','invoice_del')
                                          );

// Invoice ADD
  $actions["payment"]["invoice_add"] = array (
    'Url'      => "$path/payment/payment_index.php?action=invoice_add",
    'Right'    => $cright_write,
    'Privacy'  => true,
    'Condition'=> array ('None') 
                                          );

// Invoice Del
  $actions["payment"]["invoice_del"] = array (
    'Url'      => "$path/payment/payment_index.php?action=invoice_del",
    'Right'    => $cright_write,
    'Privacy'  => true,
    'Condition'=> array ('None') 
                                          );

// Detail Duplicate
  $actions["payment"]["detailduplicate"] = array (
     'Name'     => $l_header_duplicate,
     'Url'      => "$path/payment/payment_index.php?action=detailduplicate&amp;payment_id=".$params["payment_id"],
     'Right'    => $cright_write,
     'Condition'=> array ('detailconsult', 'detailupdate', 'update') 
                                           );

// Detail Update
  $actions["payment"]["detailupdate"] = array (
    'Name'     => $l_header_update,
    'Url'      => "$path/payment/payment_index.php?action=detailupdate&amp;payment_id=".$params["payment_id"],
    'Right'    => $cright_write,
    'Condition'=> array ('detailconsult') 
                                     	      );

// Insert
  $actions["payment"]["insert"] = array (
    'Url'      => "$path/payment/payment_index.php?action=insert",
    'Right'    => $cright_write,
    'Condition'=> array ('None')
                                                );

// Update
  $actions["payment"]["update"] = array (
    'Url'      => "$path/payment/payment_index.php?action=update",
    'Right'    => $cright_write,
    'Condition'=> array ('None') 
                                                );

// Invoice Update
  $actions["payment"]["invoice_update"] = array (
    'Url'      => "$path/payment/payment_index.php?action=invoice_update",
    'Right'    => $cright_write,
    'Condition'=> array ('None') 
                                                );

// Check Delete
  $actions["payment"]["check_delete"] = array (
    'Name'     => $l_header_delete,
    'Url'      => "$path/payment/payment_index.php?action=check_delete&amp;payment_id=".$params["payment_id"],
    'Right'    => $cright_write,
    'Condition'=> array ('detailconsult', 'detailupdate')
                                     	 );

// Delete
  $actions["payment"]["delete"] = array (
    'Url'      => "$path/payment/payment_index.php?action=delete&amp;payment_id=".$params["payment_id"],
    'Right'    => $cright_write,
    'Condition'=> array ('None')
                                     	 );

// Admin
  $actions["payment"]["admin"] = array (
    'Name'     => $l_header_admin,
    'Url'      => "$path/payment/payment_index.php?action=admin",
    'Right'    => $cright_read_admin,
    'Condition'=> array ('all') 
                                       );

// Display
  $actions["payment"]["display"] = array (
    'Name'     => $l_header_display,
    'Url'      => "$path/payment/payment_index.php?action=display",
    'Right'    => $cright_read,
    'Condition'=> array ('all') 
                                      	 );

// Display
  $actions["payment"]["dispref_display"] = array (
    'Url'      => "$path/payment/payment_index.php?action=display_dispref",
    'Right'    => $cright_read, 
    'Condition'=> array ('None') 
                                      	 );

// Display Level
  $actions["payment"]["dispref_level"] = array (
    'Url'      => "$path/payment/payment_index.php?action=display_level",
    'Right'    => $cright_read, 
    'Condition'=> array ('None') 
                                      	        );

}


///////////////////////////////////////////////////////////////////////////////
// Payment Actions updates (after processing, before displaying menu)
///////////////////////////////////////////////////////////////////////////////
function update_payment_action() {
  global $params, $actions, $path, $l_payment;

  $id = $params["payment_id"];
  if ($id > 0) {
    // Detail Consult
    $actions["payment"]["detailconsult"]["Url"] = "$path/payment/payment_index.php?action=detailconsult&amp;payment_id=$id";
    $actions["payment"]["detailconsult"]['Condition'][] = 'insert';

    // Sel invoice : Invoice selection (menu)
    $actions["payment"]["sel_invoice"]["Url"] = "$path/invoice/invoice_index.php?action=ext_get_id&amp;popup=1&amp;ext_action=invoice_add&amp;ext_url=".urlencode($path."/payment/payment_index.php?action=invoice_add&amp;payment_id=$id&amp;sel_invoice_id=")."&amp;ext_id=$id&amp;ext_target=$l_payment";
    $actions["payment"]["sel_invoice"]['Condition'][] = 'insert';

    // Invoice
    $actions["payment"]["detail_invoice"]["Url"] = "$path/payment/payment_index.php?action=detail_invoice&amp;payment_id=$id";
    $actions["payment"]["detail_invoice"]['Condition'][] = 'insert';

    // Detail Update
    $actions["payment"]["detailupdate"]['Url'] = "$path/payment/payment_index.php?action=detailupdate&amp;payment_id=$id";
    $actions["payment"]["detailupdate"]['Condition'][] = 'insert';

    // Check Delete
    $actions["payment"]["check_delete"]['Url'] = "$path/payment/payment_index.php?action=check_delete&amp;payment_id=$id";
    $actions["payment"]["check_delete"]['Condition'][] = 'insert';
  }
}

?>
