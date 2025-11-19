package com.greenhouse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;

/**
 * 邮件服务类
 * 用于发送报警邮件
 */
@Slf4j
@Service
public class EmailService {
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Value("${spring.mail.enabled:true}")
    private Boolean mailEnabled;
    
    /**
     * 发送报警邮件
     */
    public boolean sendAlertEmail(String toEmail, String subject, String content) {
        if (mailSender == null) {
            log.warn("邮件服务未配置，无法发送邮件");
            return false;
        }
        
        if (!mailEnabled || fromEmail == null || fromEmail.isEmpty()) {
            log.warn("邮件服务未启用或未配置发件人邮箱");
            return false;
        }
        
        if (toEmail == null || toEmail.isEmpty()) {
            log.warn("收件人邮箱为空");
            return false;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("成功发送报警邮件到: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("发送邮件失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送AI托管执行报告邮件（HTML格式）
     */
    public boolean sendHostingReportEmail(String toEmail, String status, List<String> actions, List<String> issues,
                                          String aiAnalysis, String aiSummary) {
        if (mailSender == null) {
            return false;
        }
        
        if (!mailEnabled || fromEmail == null || fromEmail.isEmpty()) {
            return false;
        }
        
        if (toEmail == null || toEmail.isEmpty()) {
            return false;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🌱 智能温室AI自动托管执行报告");
            
            // 生成HTML邮件内容
            String htmlContent = buildHtmlEmailContent(status, actions, issues, aiAnalysis, aiSummary);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("成功发送HTML格式的AI托管报告邮件到: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("发送HTML邮件失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 构建HTML格式的邮件内容
     */
    private String buildHtmlEmailContent(String status, List<String> actions, List<String> issues,
                                        String aiAnalysis, String aiSummary) {
        // 状态显示文本和颜色
        String statusText = getStatusText(status);
        String statusColor = getStatusColor(status);
        String statusIcon = getStatusIcon(status);
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='zh-CN'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("  * { margin: 0; padding: 0; box-sizing: border-box; }");
        html.append("  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', Arial, sans-serif; background-color: #f5f7fa; padding: 20px; }");
        html.append("  .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); overflow: hidden; }");
        html.append("  .email-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #ffffff; padding: 30px 20px; text-align: center; }");
        html.append("  .email-header h1 { font-size: 24px; font-weight: 600; margin-bottom: 10px; }");
        html.append("  .email-header p { font-size: 14px; opacity: 0.9; }");
        html.append("  .email-body { padding: 30px 20px; }");
        html.append("  .status-card { background: linear-gradient(135deg, ").append(statusColor).append(" 0%, ").append(statusColor).append("dd 100%); color: #ffffff; padding: 20px; border-radius: 8px; margin-bottom: 25px; text-align: center; }");
        html.append("  .status-card .status-icon { font-size: 48px; margin-bottom: 10px; }");
        html.append("  .status-card .status-text { font-size: 20px; font-weight: 600; }");
        html.append("  .section { margin-bottom: 25px; }");
        html.append("  .section-title { font-size: 18px; font-weight: 600; color: #2d3748; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 2px solid #e2e8f0; display: flex; align-items: center; }");
        html.append("  .section-title .icon { margin-right: 8px; font-size: 20px; }");
        html.append("  .action-list, .issue-list { list-style: none; }");
        html.append("  .action-item, .issue-item { background-color: #f7fafc; padding: 15px; margin-bottom: 10px; border-radius: 8px; border-left: 4px solid #48bb78; transition: all 0.3s ease; }");
        html.append("  .action-item:hover, .issue-item:hover { background-color: #edf2f7; transform: translateX(5px); }");
        html.append("  .action-item { border-left-color: #48bb78; }");
        html.append("  .issue-item { border-left-color: #f56565; }");
        html.append("  .item-number { display: inline-block; width: 24px; height: 24px; background-color: #48bb78; color: #ffffff; border-radius: 50%; text-align: center; line-height: 24px; font-size: 12px; font-weight: 600; margin-right: 10px; }");
        html.append("  .issue-item .item-number { background-color: #f56565; }");
        html.append("  .item-content { display: inline-block; color: #2d3748; font-size: 14px; line-height: 1.6; }");
        html.append("  .empty-state { text-align: center; padding: 30px; color: #a0aec0; font-size: 14px; }");
        html.append("  .empty-state .icon { font-size: 48px; margin-bottom: 10px; opacity: 0.5; }");
        html.append("  .email-footer { background-color: #f7fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0; }");
        html.append("  .email-footer p { color: #718096; font-size: 12px; line-height: 1.6; }");
        html.append("  .footer-note { margin-top: 15px; padding: 15px; background-color: #fff5f5; border-left: 4px solid #fc8181; border-radius: 6px; }");
        html.append("  .footer-note p { color: #c53030; font-size: 13px; margin: 0; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("  <div class='email-container'>");
        html.append("    <div class='email-header'>");
        html.append("      <h1>🌱 智能温室AI自动托管</h1>");
        html.append("      <p>执行报告</p>");
        html.append("    </div>");
        html.append("    <div class='email-body'>");
        html.append("      <div class='status-card'>");
        html.append("        <div class='status-icon'>").append(statusIcon).append("</div>");
        html.append("        <div class='status-text'>").append(statusText).append("</div>");
        html.append("      </div>");
        
        // AI分析结果
        if (aiSummary != null && !aiSummary.isEmpty()) {
            html.append("      <div class='section'>");
            html.append("        <div class='section-title'>");
            html.append("          <span class='icon'>🤖</span>");
            html.append("          <span>AI分析总结</span>");
            html.append("        </div>");
            html.append("        <div style='background-color: #f0f9ff; padding: 15px; border-radius: 8px; border-left: 4px solid #3b82f6; color: #1e40af; line-height: 1.6;'>");
            html.append(escapeHtml(aiSummary));
            html.append("        </div>");
            html.append("      </div>");
        }
        
        if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
            html.append("      <div class='section'>");
            html.append("        <div class='section-title'>");
            html.append("          <span class='icon'>📊</span>");
            html.append("          <span>AI详细分析</span>");
            html.append("        </div>");
            html.append("        <div style='background-color: #f9fafb; padding: 15px; border-radius: 8px; border-left: 4px solid #6366f1; color: #374151; line-height: 1.6; white-space: pre-wrap;'>");
            // AI分析可能包含Markdown，这里简单转义，实际可以使用Markdown解析器
            html.append(escapeHtml(aiAnalysis).replace("\n", "<br>"));
            html.append("        </div>");
            html.append("      </div>");
        }
        
        // 执行的操作
        html.append("      <div class='section'>");
        html.append("        <div class='section-title'>");
        html.append("          <span class='icon'>⚡</span>");
        html.append("          <span>执行的操作</span>");
        html.append("        </div>");
        if (actions != null && !actions.isEmpty()) {
            html.append("        <ul class='action-list'>");
            for (int i = 0; i < actions.size(); i++) {
                html.append("          <li class='action-item'>");
                html.append("            <span class='item-number'>").append(i + 1).append("</span>");
                html.append("            <span class='item-content'>").append(escapeHtml(actions.get(i))).append("</span>");
                html.append("          </li>");
            }
            html.append("        </ul>");
        } else {
            html.append("        <div class='empty-state'>");
            html.append("          <div class='icon'>✓</div>");
            html.append("          <p>本次执行未执行任何操作</p>");
            html.append("        </div>");
        }
        html.append("      </div>");
        
        // 检测到的问题
        html.append("      <div class='section'>");
        html.append("        <div class='section-title'>");
        html.append("          <span class='icon'>⚠️</span>");
        html.append("          <span>检测到的问题</span>");
        html.append("        </div>");
        if (issues != null && !issues.isEmpty()) {
            html.append("        <ul class='issue-list'>");
            for (int i = 0; i < issues.size(); i++) {
                html.append("          <li class='issue-item'>");
                html.append("            <span class='item-number'>").append(i + 1).append("</span>");
                html.append("            <span class='item-content'>").append(escapeHtml(issues.get(i))).append("</span>");
                html.append("          </li>");
            }
            html.append("        </ul>");
        } else {
            html.append("        <div class='empty-state'>");
            html.append("          <div class='icon'>✓</div>");
            html.append("          <p>未检测到任何问题</p>");
            html.append("        </div>");
        }
        html.append("      </div>");
        
        html.append("    </div>");
        html.append("    <div class='email-footer'>");
        html.append("      <p>此邮件由智能温室AI自动托管系统自动发送</p>");
        html.append("      <p>生成时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("</p>");
        if (issues != null && !issues.isEmpty()) {
            html.append("      <div class='footer-note'>");
            html.append("        <p>⚠️ 检测到问题，请及时查看系统状态并采取相应措施</p>");
            html.append("      </div>");
        }
        html.append("    </div>");
        html.append("  </div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * 获取状态文本
     */
    private String getStatusText(String status) {
        switch (status) {
            case "success":
                return "执行成功";
            case "partial":
                return "部分成功";
            case "failed":
                return "执行失败";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 获取状态颜色
     */
    private String getStatusColor(String status) {
        switch (status) {
            case "success":
                return "#48bb78";
            case "partial":
                return "#ed8936";
            case "failed":
                return "#f56565";
            default:
                return "#718096";
        }
    }
    
    /**
     * 获取状态图标
     */
    private String getStatusIcon(String status) {
        switch (status) {
            case "success":
                return "✅";
            case "partial":
                return "⚠️";
            case "failed":
                return "❌";
            default:
                return "❓";
        }
    }
    
    /**
     * HTML转义，防止XSS攻击
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

