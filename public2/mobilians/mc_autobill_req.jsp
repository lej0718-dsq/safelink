<%@page language="java" contentType="text/html; charset=EUC-KR" pageEncoding="EUC-KR"%>

<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>

<%
/****************************************************************************************
* ���ϸ� : mc_req.jsp
* �ۼ��� : ���������
* �ۼ��� : 2016.06
* ��  �� : �޴��� �ڵ����� �������� �Է� ������
* ��  �� : 0009
* ---------------------------------------------------------------------------------------
* �������� �ҽ� ���Ǻ��濡 ���� å���� ������𽺿��� å���� ���� �ʽ��ϴ�.
* �����Ǽ��� ��ȯ�� �ݵ�� ������� ������������� �����ٶ��ϴ�.
****************************************************************************************/



/*****************************************************************************************
- unique�� �ŷ���ȣ�� ���� �ŷ��Ͻ� (�и���������� ��ȸ)
*****************************************************************************************/
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
String appr_dtm = dateFormat.format(new Date());



/*****************************************************************************************
- �ʼ� �Է� �׸�
*****************************************************************************************/
String Mode			= "43";		//[   2byte ����] �޴��� �ڵ����� ��û�� ���� ������. 43 �����Ұ�!!!
String Recordkey	= "";		//[  20byte ����] ������������ (��: www.mcash.co.kr)
String Mrchid		= "";		//[   8byte ����] ������𽺿��� �ο��� ����ID. ����ID �� 8�ڸ�. (8byte ���� ����)
String Svcid		= "";		//[  12byte ����] ������𽺿��� �ο��� ����ID (12byte ���� ����)
String Prdtnm		= "";		//[  50byte ����] ��ǰ��
String Prdtprice	= "";		//[  10byte ����] ������û�ݾ�
String USERKEY		= "";		//[  15byte ����] �޴�������(�����, �޴�����ȣ, �ֹι�ȣ) ��ü�� USERKEY
String AutoBillFlag	= "";		//[   1byte ����] �ڵ���������. �ڵ����� �� "2" ����. (0: �ش����, 1: ���ʵ��, 2: 2ȸ���̻� �ŷ�)
String AutoBillKey	= "";		//[  15byte ����] ���� �Ϲݰ��� �� �߱޹��� �ڵ������� key
String AutoBillDate	= "";		//[   8byte ����] �ڵ����� ��������. (�ڵ����� 1ȸ�� �Ϲݰ�����)

String Tradeid		= Svcid + "_" + appr_dtm;	//[4byte �̻�, 40byte ����] �������ŷ���ȣ. ���� ��û �� ���� unique�� ���� �����ؾ� ��.
												//�ش� ���ÿ��� �׽�Ʈ�� ���� {������ ����ID + ��û�Ͻ�} �������� �����Ͽ���.




/*****************************************************************************************
- ���� �Է� �׸�
*****************************************************************************************/
String ReqOpt		= "";		//[  30byte ����] USERKEY ��û���� (AUTOPAY �Ǵ� PARTPAY ���� �� ���� �Ϸ� �� USERKEY ����)
String Email		= "";		//[  30byte ����] ������ e-mail
String Emailflag	= "N";		//[   1byte ����] ���� �뺸 �̸��� ������ ����
String Userid		= "";		//[  50byte ����] ������ ������ID
String Prdtcd		= "";		//[  40byte ����] ��ǰ�ڵ�. �ڵ������� ��� ��ǰ�ڵ庰 SMS������ ���� ������ �� ����ϸ� ������ ������𽺿� ����� �ʿ���.
String Item			= "";		//[   8byte ����] �������ڵ�. �̻�� �� �ݵ�� �������� ����.
String Cpcd			= "";		//[  20byte ����] ��������������key. ������ ��ü�� ��쿡�� ����.
String Sellernm		= "";		//[  50byte ����] ���Ǹ��� �̸� (���¸����� ��� �� �Ǹ��� ���� �ʼ�)
String Sellertel	= "";		//[  15byte ����] ���Ǹ��� ��ȭ��ȣ (���¸����� ��� �� �Ǹ��� ���� �ʼ�)

String Commid		= "";		//[   3byte ����] �����. USERKEY ���ÿ��� ������ �ʿ����
String No			= "";		//[  11byte ����] �޴�����ȣ. USERKEY ���ÿ��� ������ �ʿ����
String Socialno		= "";		//[  13byte ����] �ֹι�ȣ. USERKEY ���ÿ��� ������ �ʿ����


//�̸����� ������ flag�� Y�� �����Ѵ�.
if(!"".equals(Email)) {
	Emailflag = "Y";
}

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=euc-kr"/>
<title>�޴��� ���� SAMPLE PAGE</title>
<script language="javascript">
function payRequest(){
	document.payForm.submit();
}
</script>
</head>

<body>
<form name="payForm" method="post" action="mc_autobill_result.jsp">
<table border="1" width="100%">
<tr>
	<td align="center" colspan="6"><font size="15pt"><b>�޴��� �ڵ����� SAMPLE PAGE</b></font></td>
</tr>
<tr>
	<td colspan="6"><font color="red">&nbsp;������ �׸��� �ʼ� ��!!!</font></td>
</tr>
<tr>
	<td align="center"><font color="red">���� ��û ����(43 ����)</font></td>
	<td align="center"><font color="red">*Mode</font></td>
	<td><input type="text" name="Mode" id="Mode" size="30" maxlength="2" value="<%=Mode%>"></td>
	<td align="center"><font color="red">������ ������</font></td>
	<td align="center"><font color="red">*Recordkey</font></td>
	<td><input type="text" name="Recordkey" id="Recordkey" size="30" maxlength="20" value="<%=Recordkey%>"></td>
</tr>
<tr>
	<td align="center"><font color="red">�������̵�</font></td>
	<td align="center"><font color="red">*Mrchid</font></td>
	<td><input type="text" name="Mrchid" id="Mrchid" size="30" maxlength="8" value="<%=Mrchid%>"></td>
	<td align="center"><font color="red">���񽺾��̵�</font></td>
	<td align="center"><font color="red">*Svcid</font></td>
	<td><input type="text" name="Svcid" id="Svcid" size="30" maxlength="12" value="<%=Svcid%>"></td>
</tr>
<tr>
	<td align="center"><font color="red">��ǰ��</font></td>
	<td align="center"><font color="red">*Prdtnm</font></td>
	<td><input type="text" name="Prdtnm" id="Prdtnm" size="30" maxlength="50" value="<%=Prdtnm%>"></td>
	<td align="center"><font color="red">������û�ݾ�</font></td>
	<td align="center"><font color="red">*Prdtprice</font></td>
	<td><input type="text" name="Prdtprice" id="Prdtprice" size="30" maxlength="10" value="<%=Prdtprice%>"></td>
</tr>
<tr>
	<td align="center"><font color="red">�ڵ����� ����Ű</font></td>
	<td align="center"><font color="red">*USERKEY</font></td>
	<td><input type="text" name="USERKEY" id="USERKEY" size="30" maxlength="15" value="<%=USERKEY%>"></td>
	<td align="center"><font color="red">�������ŷ���ȣ</font></td>
	<td align="center"><font color="red">*Tradeid</font></td>
	<td><input type="text" name="Tradeid" id="Tradeid" size="30" maxlength="40" value="<%=Tradeid%>"></td>
</tr>
<tr>
	<td align="center"><font color="red">�ڵ����� ����</font></td>
	<td align="center"><font color="red">*AutoBillFlag</font></td>
	<td><input type="text" name="AutoBillFlag" id="AutoBillFlag" size="30" maxlength="1" value="<%=AutoBillFlag%>"></td>
	<td align="center"><font color="red">���� �ڵ����� ��������</font></td>
	<td align="center"><font color="red">*AutoBillDate</font></td>
	<td><input type="text" name="AutoBillDate" id="AutoBillDate" size="30" maxlength="8" value="<%=AutoBillDate%>"></td>
</tr>
<tr>
	<td align="center"><font color="red">�ڵ����� KEY</font></td>
	<td align="center"><font color="red">*AutoBillKey</font></td>
	<td><input type="text" name="AutoBillKey" id="AutoBillKey" size="30" maxlength="15" value="<%=AutoBillKey%>"></td>
	<td align="center">���� �뺸 �̸��� ������ ����</font></td>
	<td align="center">Emailflag</font></td>
	<td><input type="text" name="Emailflag" id="Emailflag" size="30" maxlength="1" value="<%=Emailflag%>"></td>
</tr>
<tr>
	<td align="center">USERKEY ��û ����</td>
	<td align="center">ReqOpt</td>
	<td><input type="text" name="ReqOpt" id="ReqOpt" size="30" maxlength="" value="<%=ReqOpt%>"></td>
	<td align="center">������ �̸���</td>
	<td align="center">Email</td>
	<td><input type="text" name="Email" id="Email" size="30" maxlength="30" value="<%=Email%>"></td>
</tr>
<tr>
	<td align="center">����� ID</td>
	<td align="center">Userid</td>
	<td><input type="text" name="Userid" id="Userid" size="30" maxlength="20" value="<%=Userid%>"></td>
	<td align="center">��ǰ�ڵ�</td>
	<td align="center">Prdtcd</td>
	<td><input type="text" name="Prdtcd" id="Prdtcd" size="30" maxlength="40" value="<%=Prdtcd%>"></td>
</tr>
<tr>
	<td align="center">������</td>
	<td align="center">Item</td>
	<td><input type="text" name="Item" id="Item" size="30" maxlength="8" value="<%=Item%>"></td>
	<td align="center">������ ���� ���� key</td>
	<td align="center">Cpcd</td>
	<td><input type="text" name="Cpcd" id="Cpcd" size="30" maxlength="20" value="<%=Cpcd%>"></td>
</tr>
<tr>
	<td align="center">���Ǹ��ڸ�</td>
	<td align="center">Sellernm</td>
	<td><input type="text" name="Sellernm" id="Sellernm" size="30" maxlength="50" value="<%=Sellernm%>"></td>
	<td align="center">���Ǹ��� ����ó</td>
	<td align="center">Sellertel</td>
	<td><input type="text" name="Sellertel" id="Sellertel" size="30" maxlength="15" value="<%=Sellertel%>"></td>
</tr>
<tr>
	<td align="center">�����(USERKEY ���� ����ó��)</td>
	<td align="center">Commid</td>
	<td><input type="text" name="Commid" id="Commid" size="30" maxlength="3" value="<%=Commid%>"></td>
	<td align="center">�޴�����ȣ(USERKEY ���� ����ó��)</td>
	<td align="center">No</td>
	<td><input type="text" name="No" id="No" size="30" maxlength="11" value="<%=No%>"></td>
</tr>
<tr>
	<td align="center">�ֹι�ȣ(USERKEY ���� ����ó��)</td>
	<td align="center">Socialno</td>
	<td colspan="4"><input type="text" name="Socialno" id="Socialno" size="30" maxlength="13" value="<%=Socialno%>"></td>
</tr>
<tr>
	<td align="center" colspan="6">&nbsp;</td>
</tr>
<tr>
	<td align="center" colspan="6"><input type="button" value="�����ϱ�" onclick="payRequest();"></td>
</tr>
</table>
</form>
</body>
</html>
