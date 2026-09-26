package com.specquiz.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.specquiz.entity.Certificate;
import com.specquiz.entity.Option;
import com.specquiz.entity.Question;
import com.specquiz.entity.Section;
import com.specquiz.repository.CertificationRepository;
import com.specquiz.service.CertificationService;

/**
 * Seeds at least two valid sample certifications on startup (DES-007, REQ-009).
 * Each certificate is persisted through {@link CertificationService#create} so the
 * same validation rules apply (AC-024). Idempotent within a run.
 */
@Component
@Order(1)
public class CertificationDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CertificationDataLoader.class);

    private final CertificationService service;
    private final CertificationRepository repository;

    public CertificationDataLoader(CertificationService service, CertificationRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Certifications already present; skipping bootstrap.");
            return;
        }
        service.create(buildCloudPractitioner());
        service.create(buildAiPractitioner());
        log.info("Bootstrapped {} sample certification(s).", repository.count());
    }

    // AWS Certified Cloud Practitioner — two sections (60/40), 6 questions total.
    private Certificate buildCloudPractitioner() {
        Certificate cert = new Certificate("AWS Certified Cloud Practitioner", 70, 5);

        Section cloudConcepts = new Section("Cloud Concepts", 60);
        cloudConcepts.addQuestion(q("Which AWS pricing model lets you pay only for what you use?",
                "Pay-as-you-go means you are billed for actual consumption.",
                "Pay-as-you-go", "Annual license", "Flat monthly fee", "Per-employee"));
        cloudConcepts.addQuestion(q("What does the AWS shared responsibility model describe?",
                "It splits security duties between AWS and the customer.",
                "Division of security duties between AWS and customer", "AWS support tiers",
                "How to share EC2 instances", "Billing across accounts"));
        cloudConcepts.addQuestion(q("Which benefit best describes elasticity in the cloud?",
                "Elasticity scales resources up or down with demand.",
                "Scaling resources with demand", "Fixed capacity planning",
                "Owning physical servers", "Manual monthly upgrades"));
        cert.addSection(cloudConcepts);

        Section security = new Section("Security & Compliance", 40);
        security.addQuestion(q("Which AWS service manages users and permissions?",
                "IAM controls authentication and authorization in AWS.",
                "IAM", "S3", "EC2", "CloudFront"));
        security.addQuestion(q("What is the most granular way to grant AWS permissions?",
                "IAM policies define fine-grained permissions.",
                "IAM policies", "Root account sharing", "Security groups only", "Public buckets"));
        cert.addSection(security);

        return cert;
    }

    // AWS Certified AI Practitioner — two sections (80/20), 5 questions total.
    private Certificate buildAiPractitioner() {
        Certificate cert = new Certificate("AWS Certified AI Practitioner", 75, 5);

        Section aiFundamentals = new Section("AI & ML Fundamentals", 80);
        aiFundamentals.addQuestion(q("What is supervised learning?",
                "Supervised learning trains on labeled data.",
                "Training on labeled data", "Training without any data",
                "Clustering unlabeled data", "Random guessing"));
        aiFundamentals.addQuestion(q("Which term describes AI output that is fabricated but plausible?",
                "A hallucination is confident but incorrect generated content.",
                "Hallucination", "Overfitting", "Tokenization", "Normalization"));
        aiFundamentals.addQuestion(q("What does a foundation model provide?",
                "A foundation model is a large pre-trained model adaptable to many tasks.",
                "A large pre-trained, adaptable model", "A billing dashboard",
                "A network firewall", "A storage bucket"));
        aiFundamentals.addQuestion(q("Which AWS service provides access to foundation models via API?",
                "Amazon Bedrock offers managed access to foundation models.",
                "Amazon Bedrock", "Amazon S3", "Amazon VPC", "Amazon Route 53"));
        cert.addSection(aiFundamentals);

        Section responsibleAi = new Section("Responsible AI", 20);
        responsibleAi.addQuestion(q("Why is bias mitigation important in AI systems?",
                "Reducing bias helps ensure fair and equitable outcomes.",
                "To ensure fair outcomes", "To increase model size",
                "To reduce storage cost", "To disable logging"));
        cert.addSection(responsibleAi);

        return cert;
    }

    /** Builds a question with four options; the first option is the correct one. */
    private Question q(String text, String explanation, String correct, String w1, String w2, String w3) {
        Question question = new Question(text, explanation);
        question.addOption(new Option(correct, true));
        question.addOption(new Option(w1, false));
        question.addOption(new Option(w2, false));
        question.addOption(new Option(w3, false));
        return question;
    }
}
