<html>
    <body>
<?php

if (isset($_POST['num1']) && isset($_POST['num2']))
    echo 'Sum ='.$_POST['num1'] + $_POST['num2'];
else
    echo "empty";


    if (isset($_GET['num1']) && isset($_GET['num2']))
    echo 'Sum = '.$_GET['num1'] + $_GET['num2'];
?>

</body>
</html>