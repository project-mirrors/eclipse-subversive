<html>
<head>
<title>${project.name}</title>
</head>
<body>
<div style="max-width: 900px;">
<hr />
<img src="http://download.eclipse.org/eclipse.org-common/themes/Nova/images/eclipse.png" style="float: right;"/>
<h1>Welcome!</h1>
This is an <b>Eclipse Update Site</b>. To install the software hosted on this site, please use the Eclipse Update Manager.<br/>
To learn how to install software from an update site, please carefully read <a href="http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.user/tasks/tasks-127.htm">Adding a new software site</a> from Eclipse Online-Help and follow the instructions there.
<br style="clear: both;" />
<hr />
</div>
#[[
<?php
	$pathElements = array_key_exists('dir', $_GET) ? preg_split("/\//", $_GET['dir']) : array();
	printDir($pathElements, 0);

	function printDir($pathElements, $pathDepth) {
		$rootdir = getDirectory($pathElements, $pathDepth);
		$files = readFiles("./".$rootdir);
		$dirs = readDirectories("./".$rootdir);
		sort($files);
		sort($dirs);

		echo "<ul>";

		foreach($dirs as $dir) {
			echo '<li><a href="?dir='.$rootdir.$dir.'">'.$dir.'/</a></li>';

			if(count($pathElements) > $pathDepth && $dir == $pathElements[$pathDepth]) {
				printDir($pathElements, $pathDepth+1);
			}
		}

		foreach($files as $file) {
			echo '<li>'.$file.'</li>';
		}

		echo "</ul>";
	}

	function printIndent($depth) {
		for($i=0; $i<$depth; $i++)
			echo "&nbsp;";
	}

	function getDirectory($pathElements, $pathDepth) {
		$dir = "";
		for($i=0; $i<$pathDepth; $i++) {
			$dir .= $pathElements[$i]."/";
		}
		return $dir;
	}

	function readDirectories($dir) {
		$dirs = array();
		if ($handle = opendir($dir)) {
			while (false !== ($file = readdir($handle))) {
				if($file == "." || $file=="..")
					continue;
				if(is_dir($dir.$file))
					$dirs[] = $file;
			}
		}
		return $dirs;
	}

	function readFiles($dir) {
		$files = array();
		if ($handle = opendir($dir)) {
			while (false !== ($file = readdir($handle))) {
				if($file == "." || $file=="..")
					continue;
				if(is_file($dir.$file) && !preg_match("/.*\.php/", $file))
					$files[] = $file;
			}
		}
		return $files;
	}
?>
]]#
</body>

 
 