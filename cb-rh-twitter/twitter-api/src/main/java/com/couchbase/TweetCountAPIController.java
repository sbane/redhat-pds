package com.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class TweetCountAPIController {

    @Value("#{environment['COUCHBASE_CLUSTER']}")
    private String couchbaseAddress;

    @Value("#{environment['COUCHBASE_USER']}")
    private String couchbaseUserName;

    @Value("#{environment['COUCHBASE_PASSWORD']}")
    private String couchbasePassword;

    @Value("#{environment['COUCHBASE_TWEET_BUCKET']}")
    private String bucketName;

    private static final Logger log = LoggerFactory.getLogger(TweetCountAPIController.class);

    private CouchbaseCluster cluster;
    private Bucket tweetBucket;

    @PostConstruct
    public void init() {
        if (couchbaseAddress == null) {
            log.error("Missing COUCHBASE_CLUSTER environment variable.");
            return;
        }

        //connect
        cluster = CouchbaseCluster.create(couchbaseAddress);
        cluster.authenticate(couchbaseUserName, couchbasePassword);
        tweetBucket = cluster.openBucket(bucketName);

        //create primary index, if it doesn't exist
        //tweetBucket.bucketManager().createN1qlPrimaryIndex(true, false);
        tweetBucket.query(
                N1qlQuery.simple("CREATE PRIMARY INDEX `#primary` ON `tweets` WITH { \"num_replica\" : 2 }")
        );
    }

    @CrossOrigin(origins = "*")
    @RequestMapping("/tweetcount")
    public Integer tweets() {
        //query
        N1qlQueryResult result = tweetBucket.query(
                N1qlQuery.simple("select count(*) as count from tweets where type='tweet'")
        );
        for (N1qlQueryRow row : result) {
            return row.value().getInt("count");
        }
        return 0;

    }

}
