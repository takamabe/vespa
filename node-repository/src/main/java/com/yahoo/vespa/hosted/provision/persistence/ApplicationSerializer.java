// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.provision.persistence;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.ClusterResources;
import com.yahoo.config.provision.ClusterSpec;
import com.yahoo.slime.Cursor;
import com.yahoo.slime.Inspector;
import com.yahoo.slime.ObjectTraverser;
import com.yahoo.slime.Slime;
import com.yahoo.slime.SlimeUtils;
import com.yahoo.vespa.hosted.provision.applications.Application;
import com.yahoo.vespa.hosted.provision.applications.Cluster;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Application JSON serializer
 *
 * @author bratseth
 */
public class ApplicationSerializer {

    // WARNING: Since there are multiple servers in a ZooKeeper cluster and they upgrade one by one
    //          (and rewrite all nodes on startup), changes to the serialized format must be made
    //          such that what is serialized on version N+1 can be read by version N:
    //          - ADDING FIELDS: Always ok
    //          - REMOVING FIELDS: Stop reading the field first. Stop writing it on a later version.
    //          - CHANGING THE FORMAT OF A FIELD: Don't do it bro.

    private static final String idKey = "id";
    private static final String clustersKey = "clusters";
    private static final String minResourcesKey = "min";
    private static final String maxResourcesKey = "max";
    private static final String suggestedResourcesKey = "suggested";
    private static final String targetResourcesKey = "target";
    private static final String nodesKey = "nodes";
    private static final String groupsKey = "groups";
    private static final String nodeResourcesKey = "resources";

    public static byte[] toJson(Application application) {
        Slime slime = new Slime();
        toSlime(application, slime.setObject());
        try {
            return SlimeUtils.toJsonBytes(slime);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Application fromJson(byte[] data) {
        return applicationFromSlime(SlimeUtils.jsonToSlime(data).get());
    }

    // ---------------------------------------------------------------------------------------

    private static void toSlime(Application application, Cursor object) {
        object.setString(idKey, application.id().serializedForm());
        clustersToSlime(application.clusters().values(), object.setObject(clustersKey));
    }

    private static Application applicationFromSlime(Inspector applicationObject) {
        ApplicationId id = ApplicationId.fromSerializedForm(applicationObject.field(idKey).asString());
        return new Application(id, clustersFromSlime(applicationObject.field(clustersKey)));
    }

    private static void clustersToSlime(Collection<Cluster> clusters, Cursor clustersObject) {
        clusters.forEach(cluster -> toSlime(cluster, clustersObject.setObject(cluster.id().value())));
    }

    private static Collection<Cluster> clustersFromSlime(Inspector clustersObject) {
        List<Cluster> clusters = new ArrayList<>();
        clustersObject.traverse((ObjectTraverser)(id, clusterObject) -> clusters.add(clusterFromSlime(id, clusterObject)));
        return clusters;
    }

    private static void toSlime(Cluster cluster, Cursor clusterObject) {
        toSlime(cluster.minResources(), clusterObject.setObject(minResourcesKey));
        toSlime(cluster.maxResources(), clusterObject.setObject(maxResourcesKey));
        cluster.suggestedResources().ifPresent(suggested -> toSlime(suggested, clusterObject.setObject(suggestedResourcesKey)));
        cluster.targetResources().ifPresent(target -> toSlime(target, clusterObject.setObject(targetResourcesKey)));
    }

    private static Cluster clusterFromSlime(String id, Inspector clusterObject) {
        return new Cluster(ClusterSpec.Id.from(id),
                           clusterResourcesFromSlime(clusterObject.field(minResourcesKey)),
                           clusterResourcesFromSlime(clusterObject.field(maxResourcesKey)),
                           optionalClusterResourcesFromSlime(clusterObject.field(suggestedResourcesKey)),
                           optionalClusterResourcesFromSlime(clusterObject.field(targetResourcesKey)));
    }

    private static void toSlime(ClusterResources resources, Cursor clusterResourcesObject) {
        clusterResourcesObject.setLong(nodesKey, resources.nodes());
        clusterResourcesObject.setLong(groupsKey, resources.groups());
        NodeResourcesSerializer.toSlime(resources.nodeResources(), clusterResourcesObject.setObject(nodeResourcesKey));
    }

    private static ClusterResources clusterResourcesFromSlime(Inspector clusterResourcesObject) {
        return new ClusterResources((int)clusterResourcesObject.field(nodesKey).asLong(),
                                    (int)clusterResourcesObject.field(groupsKey).asLong(),
                                    NodeResourcesSerializer.resourcesFromSlime(clusterResourcesObject.field(nodeResourcesKey)));
    }

    private static Optional<ClusterResources> optionalClusterResourcesFromSlime(Inspector clusterResourcesObject) {
        return clusterResourcesObject.valid() ? Optional.of(clusterResourcesFromSlime(clusterResourcesObject))
                                              : Optional.empty();
    }

}
