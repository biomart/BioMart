<%@ page language="java"%>
<script>
parent.${param.scope}.write("${param.uuid}", { "what": "${param.what}" });
parent.${param.scope}.done("${param.uuid}");
</script>
