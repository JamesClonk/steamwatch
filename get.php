<?php

$wishlist = $_GET["wishlist"];
$content = "";

if(preg_match("/^[0-9][0-9][0-9][0-9][0-9]*$/", $wishlist)) {
    $content = get_data('http://steamcommunity.com/profiles/' . $wishlist . '/wishlist');
} else {
    $content = get_data('http://steamcommunity.com/id/' . $wishlist . '/wishlist');
}

echo($content);

function get_data($url) {
	$ch = curl_init();
	$timeout = 10;
	curl_setopt($ch, CURLOPT_URL, $url);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, $timeout);
	$data = curl_exec($ch);
	curl_close($ch);
	return $data;
}

?>